package id.sch.mts241alikhlash.exam.data.repository

import id.sch.mts241alikhlash.exam.api.exam.apis.StudentExamsApi
import id.sch.mts241alikhlash.exam.api.exam.models.PaperChoiceDto
import id.sch.mts241alikhlash.exam.api.exam.models.PaperQuestionDto
import id.sch.mts241alikhlash.exam.api.exam.models.RecordSessionEventsRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.RecordViolationsRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.ResumeSessionRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.SaveAnswerItemDto
import id.sch.mts241alikhlash.exam.api.exam.models.SaveAnswersBatchDto
import id.sch.mts241alikhlash.exam.api.exam.models.SessionEventItemDto
import id.sch.mts241alikhlash.exam.api.exam.models.SessionHeartbeatDto
import id.sch.mts241alikhlash.exam.api.exam.models.SessionPaperDto
import id.sch.mts241alikhlash.exam.api.exam.models.SessionSuspensionRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.SubmitSessionRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.SubmitSessionResponseDto
import id.sch.mts241alikhlash.exam.data.local.AnswerDao
import id.sch.mts241alikhlash.exam.data.local.AnswerSyncState
import id.sch.mts241alikhlash.exam.data.local.CachedAnswerEntity
import id.sch.mts241alikhlash.exam.data.local.CachedQuestionEntity
import id.sch.mts241alikhlash.exam.data.local.CachedSessionEventEntity
import id.sch.mts241alikhlash.exam.data.local.NoSessionEvents
import id.sch.mts241alikhlash.exam.data.local.QuestionDao
import id.sch.mts241alikhlash.exam.data.local.SessionEventDao
import id.sch.mts241alikhlash.exam.data.model.AnswerPayload
import id.sch.mts241alikhlash.exam.data.remote.ApiErrorCode
import id.sch.mts241alikhlash.exam.data.remote.ApiException
import id.sch.mts241alikhlash.exam.data.remote.NETWORK_ERROR_MESSAGE
import id.sch.mts241alikhlash.exam.data.remote.guarded
import id.sch.mts241alikhlash.exam.data.remote.unwrapData
import id.sch.mts241alikhlash.exam.data.session.parseIsoMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

data class SyncBatchOutcome(
    val confirmed: Int,
    val pending: Int,
    val rejected: Int = 0,
    val rejectedMessage: String? = null,
    val skipped: Int = 0,
    val closedMessage: String? = null,
    val closedCode: String? = null,
    val offline: Boolean = false,
)

data class SyncReport(
    val synced: Int,
    val failed: Int,
    val rejected: Int = 0,
    val closedMessage: String? = null,
    val closedCode: String? = null,
)

data class SuspensionResult(
    val deadlineMillis: Long,
    val finalizeAtMillis: Long?,
    val extraMinutes: Int,
    val suspendedSeconds: Int,
)

fun answerClientId(
    sessionId: String,
    questionId: String,
    answer: String,
    updatedAtMillis: Long,
): String = UUID.nameUUIDFromBytes("$sessionId|$questionId|$updatedAtMillis|$answer".toByteArray()).toString()

fun freshClientId(): String = UUID.randomUUID().toString()

private val LOST_SESSION_MARKERS =
    listOf(
        "Sesi dilepas",
        "Sesi aktif di perangkat lain",
        "Sesi ujian tidak ditemukan",
    )

fun isSessionLostMessage(message: String): Boolean = LOST_SESSION_MARKERS.any { message.contains(it) }

fun isSessionLost(error: ApiException): Boolean =
    if (error.code != null) {
        error.code == ApiErrorCode.SESSION_LOST
    } else {
        isSessionLostMessage(error.message)
    }

data class ResumedSession(
    val status: String,
    val deadlineMillis: Long?,
    val requiresTokenReentry: Boolean,
)

data class EventUploadReport(
    val uploaded: Int,
    val requiresTokenReentry: Boolean,
    val lockedByViolation: Boolean,
)

const val MAX_EVENT_BATCH = 200

class ExamRepository(
    private val api: StudentExamsApi,
    private val dao: AnswerDao,
    private val questionDao: QuestionDao,
    private val eventDao: SessionEventDao = NoSessionEvents,
    private val deviceId: () -> String = { "" },
    private val now: () -> Long = { System.currentTimeMillis() },
    private val onServerTime: (String) -> Unit = {},
    private val log: (String, Throwable?) -> Unit = { message, error ->
        LOGGER.log(java.util.logging.Level.WARNING, message, error)
    },
) {
    private val sendLock = Mutex()

    private val sendDepth = AtomicInteger()

    val isSyncingOrSubmitting: Boolean get() = sendDepth.get() > 0

    private suspend fun <T> sending(block: suspend () -> T): T {
        sendDepth.incrementAndGet()
        try {
            return block()
        } finally {
            sendDepth.decrementAndGet()
        }
    }

    fun observeAnswers(sessionId: String): Flow<List<CachedAnswerEntity>> = dao.observeAnswers(sessionId)

    suspend fun getQuestions(
        sessionId: String,
        forceRefresh: Boolean = false,
    ): Result<List<PaperQuestionDto>> = loadPaper(sessionId, forceRefresh).map { it.questions }

    suspend fun loadPaper(
        sessionId: String,
        forceRefresh: Boolean = false,
    ): Result<SessionPaperDto> {
        val cached = loadCache(sessionId)
        if (!forceRefresh && cached.isNotEmpty() && !isStale(cached)) {
            return Result.success(cachedPaper(cached))
        }

        return try {
            val remote = guarded { api.sessionExamControllerQuestions(deviceId(), sessionId).unwrapData { it.data } }
            onServerTime(remote.serverTime)
            if (remote.questions.isEmpty() && cached.isNotEmpty()) {
                Result.success(cachedPaper(cached))
            } else {
                cacheQuestions(sessionId, remote.questions)
                Result.success(remote)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            if (cached.isNotEmpty()) {
                Result.success(cachedPaper(cached))
            } else {
                Result.failure(IllegalStateException(EMPTY_CACHE_MESSAGE, error))
            }
        }
    }

    private fun cachedPaper(cached: List<CachedQuestionEntity>) =
        SessionPaperDto(
            questions = cached.map { it.toDto() },
            startedAt = "",
            minSubmitMinutes = 0,
            serverTime = "",
        )

    private suspend fun cacheQuestions(
        sessionId: String,
        remote: List<PaperQuestionDto>,
    ) {
        try {
            questionDao.replaceForSession(sessionId, remote.map { it.toEntity(sessionId, now()) })
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            log("Failed to cache questions for $sessionId", error)
        }
    }

    suspend fun submit(
        sessionId: String,
        violations: Int = 0,
    ): SubmitSessionResponseDto = sending { submitLoop(sessionId, violations) }

    private suspend fun submitLoop(
        sessionId: String,
        violations: Int,
    ): SubmitSessionResponseDto {
        var attempt = 0
        while (true) {
            val pending = dao.getPendingAnswers(sessionId).filter { it.answer.isNotBlank() }
            if (pending.size > SUBMIT_PENDING_LIMIT) {
                if (attempt++ >= SUBMIT_RETRY_LIMIT) throw unsent(pending.size)
                val outcome = syncPendingBatch(sessionId)
                if (outcome.confirmed == 0) {
                    if (outcome.offline) throw ApiException(0, NETWORK_ERROR_MESSAGE)
                    throw unsent(outcome.pending)
                }
                continue
            }
            val total = dao.countAnswered(sessionId)
            try {
                val result =
                    guarded {
                        api
                            .sessionExamControllerSubmit(
                                deviceId(),
                                sessionId,
                                SubmitSessionRequestDto(
                                    violations = violations,
                                    expectedAnswerCount = total,
                                    pendingAnswers = pending.map { it.toItem() },
                                ),
                            ).unwrapData { it.data }
                    }
                if (result.answersStored) markSubmitted(pending)
                onServerTime(result.serverTime)
                return result
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: ApiException) {
                val retryable =
                    error.statusCode == 409 &&
                        (error.code == ApiErrorCode.SHORTFALL || error.code == ApiErrorCode.REBIND)
                if (!retryable || attempt++ >= SUBMIT_RETRY_LIMIT) throw error
                syncPendingBatch(sessionId)
            }
        }
    }

    private fun unsent(count: Int) = ApiException(0, "$count jawaban belum terkirim. Coba lagi.")

    private suspend fun markSubmitted(pending: List<CachedAnswerEntity>) {
        val at = now()
        for (row in pending) {
            dao.markConfirmed(row.sessionId, row.questionId, row.clientId, row.updatedAtMillis, at)
        }
    }

    suspend fun heartbeat(sessionId: String): SessionHeartbeatDto? =
        try {
            guarded { api.sessionExamControllerHeartbeatNow(deviceId(), sessionId).unwrapData { it.data } }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            log("Heartbeat failed for $sessionId", error)
            null
        }

    suspend fun reportSuspension(
        sessionId: String,
        seconds: Int,
        clientId: String,
    ): Result<SuspensionResult> =
        try {
            val dto =
                guarded {
                    api
                        .sessionExamControllerSuspension(
                            deviceId(),
                            sessionId,
                            SessionSuspensionRequestDto(SessionSuspensionRequestDto.Reason.CALL, seconds, clientId),
                        ).unwrapData { it.data }
                }
            Result.success(
                SuspensionResult(
                    deadlineMillis = parseIsoMillis(dto.deadline),
                    finalizeAtMillis = dto.finalizeAt.let { runCatching { parseIsoMillis(it) }.getOrNull() },
                    extraMinutes = dto.extraMinutes,
                    suspendedSeconds = dto.suspendedSeconds,
                ),
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            log("Failed to report a suspension for $sessionId", error)
            Result.failure(error)
        }

    suspend fun reportViolations(
        sessionId: String,
        count: Int,
    ): Int? =
        try {
            guarded {
                api.sessionExamControllerViolations(deviceId(), sessionId, RecordViolationsRequestDto(count)).unwrapData { it.data }
            }.violationCount
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            log("Failed to report violations for $sessionId", error)
            null
        }

    suspend fun clearQuestions(sessionId: String) {
        questionDao.deleteForSession(sessionId)
    }

    suspend fun recordAnswer(
        questionId: String,
        sessionId: String,
        answer: String,
        isDoubtful: Boolean = false,
    ): CachedAnswerEntity {
        val existing = dao.getAnswer(sessionId, questionId)
        if (
            existing != null &&
            existing.answer == answer &&
            existing.isDoubtful == isDoubtful &&
            existing.isPending
        ) {
            return existing
        }
        val at = now()
        val entity =
            CachedAnswerEntity(
                questionId = questionId,
                sessionId = sessionId,
                answer = answer,
                isDoubtful = isDoubtful,
                clientId = answerClientId(sessionId, questionId, answer, at),
                syncState = if (answer.isBlank()) AnswerSyncState.SERVER_CONFIRMED else AnswerSyncState.LOCAL_PENDING,
                updatedAtMillis = at,
                confirmedAtMillis = null,
            )
        dao.saveAnswer(entity)
        return entity
    }

    suspend fun syncPending(sessionId: String): SyncReport {
        val outcome = syncPendingBatch(sessionId)
        return SyncReport(
            synced = outcome.confirmed,
            failed = outcome.pending,
            rejected = outcome.rejected,
            closedMessage = outcome.closedMessage,
            closedCode = outcome.closedCode,
        )
    }

    suspend fun syncPendingBatch(
        sessionId: String,
        batteryLevel: Int? = null,
        isCharging: Boolean? = null,
        dndGranted: Boolean? = null,
        lockTaskActive: Boolean? = null,
        batteryUnrestricted: Boolean? = null,
    ): SyncBatchOutcome =
        sending {
            syncPendingBatchLoop(sessionId, batteryLevel, isCharging, dndGranted, lockTaskActive, batteryUnrestricted)
        }

    private suspend fun syncPendingBatchLoop(
        sessionId: String,
        batteryLevel: Int?,
        isCharging: Boolean?,
        dndGranted: Boolean?,
        lockTaskActive: Boolean?,
        batteryUnrestricted: Boolean?,
    ): SyncBatchOutcome {
        var confirmed = 0
        var rejected = 0
        var rejectedMessage: String? = null
        var skipped = 0
        var closedMessage: String? = null
        var closedCode: String? = null
        var offline = false

        sendLock.withLock {
            while (true) {
                val pending = dao.getPendingAnswers(sessionId)
                if (pending.isEmpty()) break
                val chunk = pending.take(MAX_BATCH_SIZE)
                for (item in chunk) {
                    dao.markSyncing(item.sessionId, item.questionId, item.clientId)
                }
                val result =
                    try {
                        guarded {
                            api
                                .sessionExamControllerSaveBatch(
                                    deviceId(),
                                    sessionId,
                                    SaveAnswersBatchDto(
                                        answers = chunk.map { it.toItem() },
                                        pendingLocalCount = pending.size - chunk.size,
                                        batteryLevel = batteryLevel,
                                        isCharging = isCharging,
                                        dndGranted = dndGranted,
                                        lockTaskActive = lockTaskActive,
                                        batteryUnrestricted = batteryUnrestricted,
                                    ),
                                ).unwrapData { it.data }
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: ApiException) {
                        for (item in chunk) {
                            dao.releaseSyncing(item.sessionId, item.questionId, item.clientId)
                        }
                        when (error.statusCode) {
                            0 -> offline = true
                            409 -> {
                                closedMessage = error.message
                                closedCode = error.code
                            }
                            400 -> {
                                rejected += chunk.size
                                rejectedMessage = error.message
                            }
                            else -> log("Failed to sync ${chunk.size} answers for $sessionId", error)
                        }
                        break
                    }

                val at = now()
                var progressed = 0
                for (item in chunk) {
                    val ack =
                        result.confirmed.firstOrNull {
                            it.questionId == item.questionId && it.clientId == item.clientId
                        }
                    if (ack == null) {
                        dao.releaseSyncing(item.sessionId, item.questionId, item.clientId)
                        continue
                    }
                    if (
                        dao.markConfirmed(
                            sessionId = item.sessionId,
                            questionId = item.questionId,
                            clientId = item.clientId,
                            updatedAtMillis = item.updatedAtMillis,
                            confirmedAtMillis = at,
                        ) > 0
                    ) {
                        confirmed++
                        progressed++
                    } else {
                        dao.releaseSyncing(item.sessionId, item.questionId, item.clientId)
                    }
                }

                var rebind = 0
                for (refused in result.skipped) {
                    val row =
                        chunk.firstOrNull {
                            it.questionId == refused.questionId && it.clientId == refused.clientId
                        } ?: continue
                    if (
                        dao.replaceClientId(
                            sessionId = row.sessionId,
                            questionId = row.questionId,
                            clientId = row.clientId,
                            nextClientId = freshClientId(),
                        ) > 0
                    ) {
                        rebind++
                    }
                }
                skipped += rebind
                onServerTime(result.serverTime)
                if (rebind > 0 && skipped <= MAX_REBIND) continue
                if (progressed == 0) break
                if (chunk.size < MAX_BATCH_SIZE) break
            }
        }

        return SyncBatchOutcome(
            confirmed = confirmed,
            pending = dao.countPendingAnswers(sessionId),
            rejected = rejected,
            rejectedMessage = rejectedMessage,
            skipped = skipped,
            closedMessage = closedMessage,
            closedCode = closedCode,
            offline = offline,
        )
    }

    suspend fun pendingCount(sessionId: String): Int = dao.countPendingAnswers(sessionId)

    suspend fun uploadSessionEvents(sessionId: String): EventUploadReport {
        val queued = eventDao.unsynced(sessionId)
        if (queued.isEmpty()) {
            return EventUploadReport(0, requiresTokenReentry = false, lockedByViolation = false)
        }
        var uploaded = 0
        var reentry = false
        var locked = false
        for (chunk in queued.chunked(MAX_EVENT_BATCH)) {
            val response =
                guarded {
                    api
                        .sessionExamControllerEvents(
                            deviceId(),
                            sessionId,
                            RecordSessionEventsRequestDto(chunk.map { it.toUploadItem() }),
                        ).unwrapData { it.data }
                }
            eventDao.markSynced(chunk.map { it.clientId })
            onServerTime(response.serverTime)
            uploaded += chunk.size
            reentry = reentry || response.requiresTokenReentry
            locked = locked || response.lockedByViolation
        }
        return EventUploadReport(uploaded, reentry, locked)
    }

    suspend fun resumeSession(
        sessionId: String,
        accessCode: String,
    ): ResumedSession {
        val result =
            guarded {
                api
                    .sessionExamControllerResume(
                        deviceId(),
                        sessionId,
                        ResumeSessionRequestDto(accessCode.trim(), deviceId()),
                    ).unwrapData { it.data }
            }
        onServerTime(result.serverTime)
        return ResumedSession(
            status = result.status.value,
            deadlineMillis = runCatching { parseIsoMillis(result.deadline) }.getOrNull(),
            requiresTokenReentry = result.requiresTokenReentry,
        )
    }

    suspend fun pullServerAnswers(sessionId: String): Int {
        val remote = guarded { api.sessionExamControllerAnswers(deviceId(), sessionId).unwrapData { it.data } }
        var imported = 0
        for (item in remote) {
            val payload = AnswerPayload.fromJson(item.answer) ?: continue
            val at = now()
            val inserted =
                dao.insertIfAbsent(
                    CachedAnswerEntity(
                        questionId = item.questionId,
                        sessionId = sessionId,
                        answer = payload.encode(),
                        isDoubtful = false,
                        clientId = answerClientId(sessionId, item.questionId, payload.encode(), at),
                        syncState = AnswerSyncState.SERVER_CONFIRMED,
                        updatedAtMillis = at,
                        confirmedAtMillis = at,
                    ),
                )
            if (inserted != -1L) imported++
        }
        return imported
    }

    private suspend fun loadCache(sessionId: String): List<CachedQuestionEntity> =
        try {
            questionDao.getForSession(sessionId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            emptyList()
        }

    private fun isStale(cached: List<CachedQuestionEntity>): Boolean {
        val newest = cached.maxOf { it.cachedAt }
        return now() - newest > CACHE_TTL_MS
    }

    companion object {
        const val EMPTY_CACHE_MESSAGE = "Tidak dapat memuat soal. Periksa koneksi."
        const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L
        const val MAX_BATCH_SIZE = 100
        const val MAX_REBIND = 100
        const val SUBMIT_PENDING_LIMIT = 250
        const val SUBMIT_RETRY_LIMIT = 1
        internal val json = Json { ignoreUnknownKeys = true }
        private val LOGGER: java.util.logging.Logger =
            java.util.logging.Logger
                .getLogger("ExamRepository")
    }
}

private fun CachedSessionEventEntity.toUploadItem() =
    SessionEventItemDto(
        clientId = clientId,
        type = SessionEventItemDto.Type.valueOf(type),
        isViolation = isViolation,
        detail = detail,
        occurredAt = isoFromMillis(occurredAtMs),
    )

private fun isoFromMillis(epochMillis: Long): String =
    java.text
        .SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.ROOT)
        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
        .format(java.util.Date(epochMillis))

private fun CachedAnswerEntity.toItem() =
    SaveAnswerItemDto(
        questionId = questionId,
        clientId = clientId,
        answer = (AnswerPayload.decode(answer) ?: AnswerPayload.Choice(answer)).toJson(),
    )

private fun PaperQuestionDto.toEntity(
    sessionId: String,
    cachedAt: Long,
) = CachedQuestionEntity(
    sessionId = sessionId,
    id = id,
    number = number,
    type = type.value,
    bodyHtml = bodyHtml,
    points = points,
    choicesJson =
        choices?.let {
            ExamRepository.json.encodeToString(ListSerializer(PaperChoiceDto.serializer()), it)
        },
    cachedAt = cachedAt,
)

private fun CachedQuestionEntity.toDto() =
    PaperQuestionDto(
        id = id,
        number = number,
        type = PaperQuestionDto.Type.valueOf(type),
        bodyHtml = bodyHtml,
        points = points,
        choices =
            choicesJson?.let { encoded ->
                runCatching {
                    ExamRepository.json.decodeFromString(ListSerializer(PaperChoiceDto.serializer()), encoded)
                }.getOrNull()
            },
    )
