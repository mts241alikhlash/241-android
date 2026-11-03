package id.sch.mts241alikhlash.cbt.data.repository

import id.sch.mts241alikhlash.cbt.EmptyQuestionDao
import id.sch.mts241alikhlash.cbt.InMemoryAnswerDao
import id.sch.mts241alikhlash.cbt.batchAck
import id.sch.mts241alikhlash.cbt.paperOf
import id.sch.mts241alikhlash.cbt.data.model.PaperQuestionDto
import id.sch.mts241alikhlash.cbt.data.model.RecordSessionEventsRequest
import id.sch.mts241alikhlash.cbt.data.model.RecordSessionEventsResponse
import id.sch.mts241alikhlash.cbt.data.model.ResumeSessionRequest
import id.sch.mts241alikhlash.cbt.data.model.ResumedSessionDto
import id.sch.mts241alikhlash.cbt.data.model.SaveAnswersBatchRequest
import id.sch.mts241alikhlash.cbt.data.model.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.cbt.data.model.SavedAnswerDto
import id.sch.mts241alikhlash.cbt.data.model.SubmitRequest
import id.sch.mts241alikhlash.cbt.data.model.SubmittedSessionDto
import id.sch.mts241alikhlash.cbt.data.model.ViolationCountDto
import id.sch.mts241alikhlash.cbt.data.model.ViolationReport
import id.sch.mts241alikhlash.cbt.data.remote.ExamApiService
import id.sch.mts241alikhlash.cbt.pendingAnswer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.IOException

class SyncPendingTest {

    @Test
    fun `counts confirmed and still-pending answers`() = runTest {
        val dao = InMemoryAnswerDao(pendingAnswer("q1"), pendingAnswer("q2"))
        val api = SyncFakeApi(failOnSecondBatch = true)

        val report = ExamRepository(api, dao, EmptyQuestionDao()).syncPending("s1")

        assertEquals(0, report.synced)
        assertEquals(2, report.failed)
    }

    @Test
    fun `a retry of the same revision sends the same client id`() = runTest {
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        val api = SyncFakeApi(failOnSecondBatch = false, failFirst = true)

        ExamRepository(api, dao, EmptyQuestionDao()).syncPending("s1")
        ExamRepository(api, dao, EmptyQuestionDao()).syncPending("s1")

        assertEquals(2, api.batches.size)
        assertEquals(api.batches[0].answers.single().clientId, api.batches[1].answers.single().clientId)
        assertEquals("c-q1", api.batches[0].answers.single().clientId)
    }

    @Test
    fun `a newer revision of the same answer gets a different client id`() {
        assertNotEquals(
            answerClientId("s1", "q1", "a", 1_000L),
            answerClientId("s1", "q1", "a", 2_000L)
        )
    }

    @Test
    fun `a different answer at the same timestamp gets a different client id`() {
        assertNotEquals(
            answerClientId("s1", "q1", "A", 1_000L),
            answerClientId("s1", "q1", "C", 1_000L)
        )
    }

    @Test
    fun `zero pending answers reports a clean gate`() = runTest {
        val dao = InMemoryAnswerDao()
        val api = SyncFakeApi()

        val report = ExamRepository(api, dao, EmptyQuestionDao()).syncPending("s1")

        assertEquals(0, report.synced)
        assertEquals(0, report.failed)
        assertEquals(0, api.batches.size)
    }

    @Test
    fun `the failed count comes from the queue, not the attempt snapshot`() = runTest {
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        val api = SyncFakeApi()
        val repository = ExamRepository(api, dao, EmptyQuestionDao())

        repository.syncPending("s1")
        dao.saveAnswer(pendingAnswer("q2", updatedAtMillis = 2_000L))

        assertEquals(1, dao.countPendingAnswers("s1"))
        assertEquals(1, repository.pendingCount("s1"))
    }

    @Test
    fun `an offline pass leaves every row pending`() = runTest {
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        val api = SyncFakeApi(offline = true)

        val report = ExamRepository(api, dao, EmptyQuestionDao()).syncPending("s1")

        assertEquals(1, report.failed)
        assertEquals(0, report.rejected)
        assertEquals(null, report.closedMessage)
    }
}

private class SyncFakeApi(
    private val failOnSecondBatch: Boolean = false,
    private var failFirst: Boolean = false,
    private val offline: Boolean = false
) : ExamApiService {
        override suspend fun sessionHeartbeat(
            sessionId: String
        ): id.sch.mts241alikhlash.cbt.data.model.SessionHeartbeatResponse =
            id.sch.mts241alikhlash.cbt.data.model.SessionHeartbeatResponse()

    val batches = mutableListOf<SaveAnswersBatchRequest>()

    override suspend fun getQuestions(sessionId: String) = paperOf()

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchRequest
    ): SaveAnswersBatchResultDto {
        batches += request
        if (offline) throw IOException("offline")
        if (failFirst) {
            failFirst = false
            throw IOException("offline")
        }
        if (failOnSecondBatch) throw IOException("offline")
        return batchAck(request)
    }

    override suspend fun submit(sessionId: String, request: SubmitRequest): SubmittedSessionDto =
        SubmittedSessionDto("SUBMITTED")

    override suspend fun recordSessionEvents(
        sessionId: String,
        request: RecordSessionEventsRequest
    ): RecordSessionEventsResponse = RecordSessionEventsResponse()

    override suspend fun resumeSession(
        sessionId: String,
        request: ResumeSessionRequest
    ): ResumedSessionDto = ResumedSessionDto(
        sessionId = sessionId,
        status = "IN_PROGRESS",
        deadline = "",
        serverTime = ""
    )

    override suspend fun reportViolations(
        sessionId: String,
        request: ViolationReport
    ): ViolationCountDto = ViolationCountDto(request.count)
}
