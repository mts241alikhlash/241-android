package id.sch.mts241alikhlash.istiqra.data.session

import id.sch.mts241alikhlash.istiqra.data.repository.ExamCatalogRepository
import id.sch.mts241alikhlash.istiqra.data.repository.StartedSession
import kotlin.coroutines.cancellation.CancellationException

suspend fun startExamSession(
    catalog: ExamCatalogRepository,
    sessions: SessionRepository,
    examId: String,
    accessCode: String,
    pullAnswers: suspend (String) -> Unit = {},
): StartedSession {
    val started = catalog.startSession(examId, accessCode)
    sessions.start(started.sessionId, started.deadlineLocalMillis, started.policy)
    ExamRuntimePolicy.apply(started.policy)
    try {
        pullAnswers(started.sessionId)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
    }
    return started
}
