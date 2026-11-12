package id.sch.mts241alikhlash.exam.data.repository

import id.sch.mts241alikhlash.exam.EmptyQuestionDao
import id.sch.mts241alikhlash.exam.InMemoryAnswerDao
import id.sch.mts241alikhlash.exam.StubExamApi
import id.sch.mts241alikhlash.exam.api.exam.models.RecordSessionEventsRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.RecordSessionEventsResponseDto
import id.sch.mts241alikhlash.exam.api.exam.models.RecordViolationsRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.ResumeSessionRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.ResumeSessionResponseDto
import id.sch.mts241alikhlash.exam.api.exam.models.SaveAnswersBatchDto
import id.sch.mts241alikhlash.exam.api.exam.models.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.exam.api.exam.models.SavedAnswerDto
import id.sch.mts241alikhlash.exam.api.exam.models.SubmitSessionRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.SubmitSessionResponseDto
import id.sch.mts241alikhlash.exam.api.exam.models.ViolationCountDto
import id.sch.mts241alikhlash.exam.batchAck
import id.sch.mts241alikhlash.exam.eventsResponse
import id.sch.mts241alikhlash.exam.heartbeatOf
import id.sch.mts241alikhlash.exam.paperOf
import id.sch.mts241alikhlash.exam.pendingAnswer
import id.sch.mts241alikhlash.exam.submitted
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.IOException

class SyncPendingTest {
    @Test
    fun `counts confirmed and still-pending answers`() =
        runTest {
            val dao = InMemoryAnswerDao(pendingAnswer("q1"), pendingAnswer("q2"))
            val api = SyncFakeApi(failOnSecondBatch = true)

            val report = ExamRepository(api, dao, EmptyQuestionDao()).syncPending("s1")

            assertEquals(0, report.synced)
            assertEquals(2, report.failed)
        }

    @Test
    fun `a retry of the same revision sends the same client id`() =
        runTest {
            val dao = InMemoryAnswerDao(pendingAnswer("q1"))
            val api = SyncFakeApi(failOnSecondBatch = false, failFirst = true)

            ExamRepository(api, dao, EmptyQuestionDao()).syncPending("s1")
            ExamRepository(api, dao, EmptyQuestionDao()).syncPending("s1")

            assertEquals(2, api.batches.size)
            assertEquals(
                api.batches[0]
                    .answers
                    .single()
                    .clientId,
                api.batches[1]
                    .answers
                    .single()
                    .clientId,
            )
            assertEquals(
                "c-q1",
                api.batches[0]
                    .answers
                    .single()
                    .clientId,
            )
        }

    @Test
    fun `a newer revision of the same answer gets a different client id`() {
        assertNotEquals(
            answerClientId("s1", "q1", "a", 1_000L),
            answerClientId("s1", "q1", "a", 2_000L),
        )
    }

    @Test
    fun `a different answer at the same timestamp gets a different client id`() {
        assertNotEquals(
            answerClientId("s1", "q1", "A", 1_000L),
            answerClientId("s1", "q1", "C", 1_000L),
        )
    }

    @Test
    fun `zero pending answers reports a clean gate`() =
        runTest {
            val dao = InMemoryAnswerDao()
            val api = SyncFakeApi()

            val report = ExamRepository(api, dao, EmptyQuestionDao()).syncPending("s1")

            assertEquals(0, report.synced)
            assertEquals(0, report.failed)
            assertEquals(0, api.batches.size)
        }

    @Test
    fun `the failed count comes from the queue, not the attempt snapshot`() =
        runTest {
            val dao = InMemoryAnswerDao(pendingAnswer("q1"))
            val api = SyncFakeApi()
            val repository = ExamRepository(api, dao, EmptyQuestionDao())

            repository.syncPending("s1")
            dao.saveAnswer(pendingAnswer("q2", updatedAtMillis = 2_000L))

            assertEquals(1, dao.countPendingAnswers("s1"))
            assertEquals(1, repository.pendingCount("s1"))
        }

    @Test
    fun `an offline pass leaves every row pending`() =
        runTest {
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
    private val offline: Boolean = false,
) : StubExamApi() {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.exam.api.exam.models.SessionHeartbeatDto = heartbeatOf()

    override suspend fun getQuestions(sessionId: String) = paperOf()

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchDto,
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
        request: id.sch.mts241alikhlash.exam.api.exam.models.SessionSuspensionRequestDto,
    ): id.sch.mts241alikhlash.exam.api.exam.models.SessionSuspensionDto = throw UnsupportedOperationException("not used")
}
