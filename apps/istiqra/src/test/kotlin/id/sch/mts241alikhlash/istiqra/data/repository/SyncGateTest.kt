package id.sch.mts241alikhlash.istiqra.data.repository

import id.sch.mts241alikhlash.istiqra.EmptyQuestionDao
import id.sch.mts241alikhlash.istiqra.InMemoryAnswerDao
import id.sch.mts241alikhlash.istiqra.StubExamApi
import id.sch.mts241alikhlash.istiqra.api.exam.models.RecordSessionEventsRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.RecordSessionEventsResponseDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.RecordViolationsRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.ResumeSessionRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.ResumeSessionResponseDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswersBatchDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SavedAnswerDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SubmitSessionRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SubmitSessionResponseDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.ViolationCountDto
import id.sch.mts241alikhlash.istiqra.batchAck
import id.sch.mts241alikhlash.istiqra.data.local.AnswerSyncState
import id.sch.mts241alikhlash.istiqra.eventsResponse
import id.sch.mts241alikhlash.istiqra.heartbeatOf
import id.sch.mts241alikhlash.istiqra.paperOf
import id.sch.mts241alikhlash.istiqra.pendingAnswer
import id.sch.mts241alikhlash.istiqra.submitted
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

private class BlockingApi : StubExamApi() {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.istiqra.api.exam.models.SessionHeartbeatDto = heartbeatOf()

    private val started = CompletableDeferred<Unit>()
    private val release = CompletableDeferred<Unit>()

    suspend fun awaitBatchStarted() = started.await()

    fun completeBatch() = release.complete(Unit)

    override suspend fun getQuestions(sessionId: String) = paperOf()

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchDto,
    ): SaveAnswersBatchResultDto {
        started.complete(Unit)
        release.await()
        return batchAck(request)
    }

    override suspend fun submit(
        sessionId: String,
        request: SubmitSessionRequestDto,
    ): SubmitSessionResponseDto = submitted(SubmitSessionResponseDto.Status.SUBMITTED)

    override suspend fun recordSessionEvents(
        sessionId: String,
        request: RecordSessionEventsRequestDto,
    ): RecordSessionEventsResponseDto = eventsResponse()

    override suspend fun resumeSession(
        sessionId: String,
        request: ResumeSessionRequestDto,
    ): ResumeSessionResponseDto =
        ResumeSessionResponseDto(
            sessionId = sessionId,
            status = ResumeSessionResponseDto.Status.IN_PROGRESS,
            deadline = "",
            serverTime = "",
            requiresTokenReentry = false,
        )

    override suspend fun reportViolations(
        sessionId: String,
        request: RecordViolationsRequestDto,
    ): ViolationCountDto = ViolationCountDto(request.count)

    override suspend fun reportSuspension(
        sessionId: String,
        request: id.sch.mts241alikhlash.istiqra.api.exam.models.SessionSuspensionRequestDto,
    ): id.sch.mts241alikhlash.istiqra.api.exam.models.SessionSuspensionDto = throw UnsupportedOperationException("not used")
}

private class UploadingApi : StubExamApi() {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.istiqra.api.exam.models.SessionHeartbeatDto = heartbeatOf()

    override suspend fun getQuestions(sessionId: String) = paperOf()

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchDto,
    ): SaveAnswersBatchResultDto = batchAck(request)

    override suspend fun submit(
        sessionId: String,
        request: SubmitSessionRequestDto,
    ): SubmitSessionResponseDto = submitted(SubmitSessionResponseDto.Status.SUBMITTED)

    override suspend fun recordSessionEvents(
        sessionId: String,
        request: RecordSessionEventsRequestDto,
    ): RecordSessionEventsResponseDto = eventsResponse()

    override suspend fun resumeSession(
        sessionId: String,
        request: ResumeSessionRequestDto,
    ): ResumeSessionResponseDto =
        ResumeSessionResponseDto(
            sessionId = sessionId,
            status = ResumeSessionResponseDto.Status.IN_PROGRESS,
            deadline = "",
            serverTime = "",
            requiresTokenReentry = false,
        )

    override suspend fun reportViolations(
        sessionId: String,
        request: RecordViolationsRequestDto,
    ): ViolationCountDto = ViolationCountDto(request.count)

    override suspend fun reportSuspension(
        sessionId: String,
        request: id.sch.mts241alikhlash.istiqra.api.exam.models.SessionSuspensionRequestDto,
    ): id.sch.mts241alikhlash.istiqra.api.exam.models.SessionSuspensionDto = throw UnsupportedOperationException("not used")
}
