package id.sch.mts241alikhlash.exam.data.repository

import id.sch.mts241alikhlash.exam.EmptyQuestionDao
import id.sch.mts241alikhlash.exam.InMemoryAnswerDao
import id.sch.mts241alikhlash.exam.batchAck
import id.sch.mts241alikhlash.exam.data.local.AnswerSyncState
import id.sch.mts241alikhlash.exam.data.model.RecordSessionEventsRequest
import id.sch.mts241alikhlash.exam.data.model.RecordSessionEventsResponse
import id.sch.mts241alikhlash.exam.data.model.ResumeSessionRequest
import id.sch.mts241alikhlash.exam.data.model.ResumedSessionDto
import id.sch.mts241alikhlash.exam.data.model.SaveAnswersBatchRequest
import id.sch.mts241alikhlash.exam.data.model.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.exam.data.model.SavedAnswerDto
import id.sch.mts241alikhlash.exam.data.model.SubmitRequest
import id.sch.mts241alikhlash.exam.data.model.SubmittedSessionDto
import id.sch.mts241alikhlash.exam.data.model.ViolationCountDto
import id.sch.mts241alikhlash.exam.data.model.ViolationReport
import id.sch.mts241alikhlash.exam.data.remote.ExamApiService
import id.sch.mts241alikhlash.exam.paperOf
import id.sch.mts241alikhlash.exam.pendingAnswer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncGateTest {
    @Test
    fun `answer changed during the batch stays pending`() =
        runTest {
            val dao = InMemoryAnswerDao(pendingAnswer("q1"))
            val api = BlockingApi()
            val repository = ExamRepository(api, dao, EmptyQuestionDao(), now = { 1_000L })

            val job = async { repository.syncPendingBatch("s1") }
            api.awaitBatchStarted()

            dao.saveAnswer(
                pendingAnswer("q1", answer = """{"key":"B"}""", updatedAtMillis = 2_000L, clientId = "c-q1-v2"),
            )
            api.completeBatch()

            val outcome = job.await()
            assertEquals("edited answer must remain pending", 1, outcome.pending)
            assertEquals(0, outcome.confirmed)
            assertEquals("""{"key":"B"}""", dao.rows.getValue("q1").answer)
            assertEquals(AnswerSyncState.LOCAL_PENDING, dao.rows.getValue("q1").syncState)
        }

    @Test
    fun `markConfirmed ignores a stale revision of the same answer`() =
        runTest {
            val dao = InMemoryAnswerDao(pendingAnswer("q1", updatedAtMillis = 2_000L))

            val updated = dao.markConfirmed("s1", "q1", "c-q1", 1_000L, 3_000L)

            assertEquals(0, updated)
            assertEquals(AnswerSyncState.LOCAL_PENDING, dao.rows.getValue("q1").syncState)
        }

    @Test
    fun `markConfirmed does not cross sessions`() =
        runTest {
            val dao = InMemoryAnswerDao(pendingAnswer("q1"))

            val updated = dao.markConfirmed("s2", "q1", "c-q1", 1_000L, 3_000L)

            assertEquals(0, updated)
            assertEquals(AnswerSyncState.LOCAL_PENDING, dao.rows.getValue("q1").syncState)
        }

    @Test
    fun `the sync gate opens once every pending answer is confirmed`() =
        runTest {
            val dao = InMemoryAnswerDao(pendingAnswer("q1"))
            val repository = ExamRepository(UploadingApi(), dao, EmptyQuestionDao(), now = { 1_000L })

            val outcome = repository.syncPendingBatch("s1")

            assertEquals(1, outcome.confirmed)
            assertEquals(0, outcome.pending)
            assertEquals(AnswerSyncState.SERVER_CONFIRMED, dao.rows.getValue("q1").syncState)
        }
}

private class BlockingApi : ExamApiService {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.exam.data.model.SessionHeartbeatResponse =
        id.sch.mts241alikhlash.exam.data.model
            .SessionHeartbeatResponse()

    private val started = CompletableDeferred<Unit>()
    private val release = CompletableDeferred<Unit>()

    suspend fun awaitBatchStarted() = started.await()

    fun completeBatch() = release.complete(Unit)

    override suspend fun getQuestions(sessionId: String) = paperOf()

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchRequest,
    ): SaveAnswersBatchResultDto {
        started.complete(Unit)
        release.await()
        return batchAck(request)
    }

    override suspend fun submit(
        sessionId: String,
        request: SubmitRequest,
    ): SubmittedSessionDto = SubmittedSessionDto("SUBMITTED")

    override suspend fun recordSessionEvents(
        sessionId: String,
        request: RecordSessionEventsRequest,
    ): RecordSessionEventsResponse = RecordSessionEventsResponse()

    override suspend fun resumeSession(
        sessionId: String,
        request: ResumeSessionRequest,
    ): ResumedSessionDto =
        ResumedSessionDto(
            sessionId = sessionId,
            status = "IN_PROGRESS",
            deadline = "",
            serverTime = "",
        )

    override suspend fun reportViolations(
        sessionId: String,
        request: ViolationReport,
    ): ViolationCountDto = ViolationCountDto(request.count)

    override suspend fun reportSuspension(
        sessionId: String,
        request: id.sch.mts241alikhlash.exam.data.model.SuspensionRequest,
    ): id.sch.mts241alikhlash.exam.data.model.SuspensionResponseDto = throw UnsupportedOperationException("not used")
}

private class UploadingApi : ExamApiService {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.exam.data.model.SessionHeartbeatResponse =
        id.sch.mts241alikhlash.exam.data.model
            .SessionHeartbeatResponse()

    override suspend fun getQuestions(sessionId: String) = paperOf()

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchRequest,
    ): SaveAnswersBatchResultDto = batchAck(request)

    override suspend fun submit(
        sessionId: String,
        request: SubmitRequest,
    ): SubmittedSessionDto = SubmittedSessionDto("SUBMITTED")

    override suspend fun recordSessionEvents(
        sessionId: String,
        request: RecordSessionEventsRequest,
    ): RecordSessionEventsResponse = RecordSessionEventsResponse()

    override suspend fun resumeSession(
        sessionId: String,
        request: ResumeSessionRequest,
    ): ResumedSessionDto =
        ResumedSessionDto(
            sessionId = sessionId,
            status = "IN_PROGRESS",
            deadline = "",
            serverTime = "",
        )

    override suspend fun reportViolations(
        sessionId: String,
        request: ViolationReport,
    ): ViolationCountDto = ViolationCountDto(request.count)

    override suspend fun reportSuspension(
        sessionId: String,
        request: id.sch.mts241alikhlash.exam.data.model.SuspensionRequest,
    ): id.sch.mts241alikhlash.exam.data.model.SuspensionResponseDto = throw UnsupportedOperationException("not used")
}
