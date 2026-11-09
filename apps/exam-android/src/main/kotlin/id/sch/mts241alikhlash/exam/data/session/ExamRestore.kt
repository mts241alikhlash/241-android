package id.sch.mts241alikhlash.exam.data.session

import id.sch.mts241alikhlash.exam.data.remote.ApiErrorCode
import id.sch.mts241alikhlash.exam.data.remote.ApiException
import id.sch.mts241alikhlash.exam.data.repository.SyncReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

data class RestoredSession(
    val sessionId: String,
    val deadline: Long?,
)

sealed interface RestoreOutcome {
    data object NoSession : RestoreOutcome

    data class Restored(
        val session: RestoredSession,
    ) : RestoreOutcome

    data class TokenReentry(
        val session: RestoredSession,
    ) : RestoreOutcome

    data class AlreadyFinished(
        val message: String,
    ) : RestoreOutcome

    data class Failed(
        val message: String,
    ) : RestoreOutcome

    data class SessionLost(
        val message: String,
    ) : RestoreOutcome
}

const val RESTORE_CHECK_TIMEOUT_MS = 3_000L
const val RESTORE_ERROR_MESSAGE = "Gagal memulihkan sesi ujian. Coba lagi."
const val LOST_SESSION_MESSAGE =
    "Sesi ujian ini sudah tidak berlaku di server. Kembali ke daftar ujian untuk memulai lagi."
const val FINISHED_SESSION_MESSAGE = "Ujian ini sudah dikumpulkan dan tidak bisa dibuka lagi."

suspend fun restoreExamSession(
    sessionRepository: SessionRepository,
    scope: CoroutineScope,
    syncPending: suspend (String) -> SyncReport,
    validate: suspend (String) -> Unit = {},
    syncClock: suspend (String) -> Unit = {},
    isSessionFinished: suspend (String) -> Boolean = { false },
    requiresTokenReentry: suspend (String) -> Boolean = { false },
    onSyncResult: (Result<SyncReport>) -> Unit = {},
): RestoreOutcome {
    val session =
        try {
            val id = sessionRepository.current() ?: return RestoreOutcome.NoSession
            runCatching { sessionRepository.applyStoredPolicy() }
            RestoredSession(id, sessionRepository.deadline())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return RestoreOutcome.Failed(RESTORE_ERROR_MESSAGE)
        }

    val lost = sessionLostMessage(session.sessionId, validate)
    if (lost != null) {
        sessionRepository.finishIfCurrent(session.sessionId)
        return RestoreOutcome.SessionLost(lost)
    }

    if (sessionFinished(session.sessionId, isSessionFinished)) {
        sessionRepository.finishIfCurrent(session.sessionId)
        return RestoreOutcome.AlreadyFinished(FINISHED_SESSION_MESSAGE)
    }

    syncClockQuietly(session.sessionId, syncClock)
    syncPendingInBackground(scope, session.sessionId, syncPending, onSyncResult)
    return if (tokenReentryNeeded(session.sessionId, requiresTokenReentry)) {
        RestoreOutcome.TokenReentry(session)
    } else {
        RestoreOutcome.Restored(session)
    }
}

fun syncPendingInBackground(
    scope: CoroutineScope,
    sessionId: String,
    syncPending: suspend (String) -> SyncReport,
    onResult: (Result<SyncReport>) -> Unit = {},
): Job =
    scope.launch {
        val result =
            try {
                Result.success(syncPending(sessionId))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Result.failure(error)
            }
        onResult(result)
    }

private suspend fun tokenReentryNeeded(
    sessionId: String,
    requiresTokenReentry: suspend (String) -> Boolean,
): Boolean =
    try {
        requiresTokenReentry(sessionId)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        false
    }

private suspend fun sessionFinished(
    sessionId: String,
    isSessionFinished: suspend (String) -> Boolean,
): Boolean =
    try {
        withTimeoutOrNull(RESTORE_CHECK_TIMEOUT_MS) { isSessionFinished(sessionId) } ?: false
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        false
    }

private suspend fun syncClockQuietly(
    sessionId: String,
    syncClock: suspend (String) -> Unit,
) {
    try {
        withTimeoutOrNull(RESTORE_CHECK_TIMEOUT_MS) { syncClock(sessionId) }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
    }
}

private suspend fun sessionLostMessage(
    sessionId: String,
    validate: suspend (String) -> Unit,
): String? =
    try {
        withTimeoutOrNull(RESTORE_CHECK_TIMEOUT_MS) { validate(sessionId) }
        null
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: ApiException) {
        when {
            error.code == ApiErrorCode.SESSION_LOST -> error.message
            error.statusCode == 404 -> error.message
            error.statusCode == 409 -> error.message
            error.statusCode == 401 -> LOST_SESSION_MESSAGE
            error.statusCode == 403 -> LOST_SESSION_MESSAGE
            else -> null
        }
    } catch (_: Exception) {
        null
    }
