package id.sch.mts241alikhlash.exam.ui.screens

import id.sch.mts241alikhlash.exam.InMemoryAnswerDao
import id.sch.mts241alikhlash.exam.MainDispatcherRule
import id.sch.mts241alikhlash.exam.PaperExamApi
import id.sch.mts241alikhlash.exam.StubExamApi
import id.sch.mts241alikhlash.exam.batchAck
import id.sch.mts241alikhlash.exam.choices
import id.sch.mts241alikhlash.exam.data.local.AnswerSyncState
import id.sch.mts241alikhlash.exam.data.model.PaperQuestionDto
import id.sch.mts241alikhlash.exam.data.model.RecordSessionEventsRequest
import id.sch.mts241alikhlash.exam.data.model.RecordSessionEventsResponse
import id.sch.mts241alikhlash.exam.data.model.ResumeSessionRequest
import id.sch.mts241alikhlash.exam.data.model.ResumedSessionDto
import id.sch.mts241alikhlash.exam.data.model.SaveAnswersBatchRequest
import id.sch.mts241alikhlash.exam.data.model.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.exam.data.model.SavedAnswerDto
import id.sch.mts241alikhlash.exam.data.model.SessionPaperDto
import id.sch.mts241alikhlash.exam.data.model.SubmitRequest
import id.sch.mts241alikhlash.exam.data.model.SubmittedSessionDto
import id.sch.mts241alikhlash.exam.data.model.ViolationCountDto
import id.sch.mts241alikhlash.exam.data.model.ViolationReport
import id.sch.mts241alikhlash.exam.data.remote.ExamApiService
import id.sch.mts241alikhlash.exam.data.repository.ExamRepository
import id.sch.mts241alikhlash.exam.paperOf
import id.sch.mts241alikhlash.exam.pendingAnswer
import id.sch.mts241alikhlash.exam.testClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ExamViewModelSubmitTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `submit goes through in one trip with the pending answers`() =
        runTest {
            val api = PaperExamApi()
            val vm = viewModel(repo(dao = InMemoryAnswerDao(*Array(46) { pendingAnswer("q$it") }), api = api))
            runCurrent()

            vm.submitExam()
            runCurrent()

            assertTrue(vm.uiState.value.isSubmitted)
            assertNull(vm.uiState.value.submitError)
            val request = api.submits.single()
            assertEquals(46, request.pendingAnswers?.size)
            assertEquals(46, request.expectedAnswerCount)
        }

    @Test
    fun `submit succeeds when nothing is pending`() =
        runTest {
            val vm = viewModel(repo(api = PaperExamApi()))
            runCurrent()

            vm.submitExam()
            runCurrent()

            assertTrue(vm.uiState.value.isSubmitted)
            assertNull(vm.uiState.value.submitError)
        }

    @Test
    fun `submitting flag is set while the submit is in flight`() =
        runTest {
            val api = BlockingSubmitApi()
            val vm = viewModel(repo(dao = InMemoryAnswerDao(pendingAnswer("q1")), api = api))
            runCurrent()

            vm.submitExam()
            api.awaitSubmitStarted()

            assertTrue("isSubmitting must be true during submit", vm.uiState.value.isSubmitting)
            assertFalse(vm.uiState.value.isSubmitted)
            assertFalse("inputs must be disabled while submitting", vm.uiState.value.canAnswer)
            assertFalse("submit must be disabled while submitting", vm.uiState.value.canSubmit)

            api.completeSubmit()
            runCurrent()
            assertFalse(vm.uiState.value.isSubmitting)
        }

    @Test
    fun `concurrent submit taps only run one submit`() =
        runTest {
            val api = BlockingSubmitApi()
            val vm = viewModel(repo(dao = InMemoryAnswerDao(pendingAnswer("q1")), api = api))
            runCurrent()

            vm.submitExam()
            api.awaitSubmitStarted()
            vm.submitExam()
            vm.submitExam()

            api.completeSubmit()
            runCurrent()

            assertEquals("only one submit may be in flight", 1, api.submitCalls)
        }

    @Test
    fun `submit failure surfaces a message instead of crashing`() =
        runTest {
            val vm = viewModel(repo(api = FailingApi()))
            runCurrent()

            vm.submitExam()
            runCurrent()

            assertFalse(vm.uiState.value.isSubmitted)
            assertFalse(vm.uiState.value.isSubmitting)
            assertEquals("Tidak dapat terhubung ke server.", vm.uiState.value.submitError)
        }

    @Test
    fun `submit is rejected while the paper is still loading`() =
        runTest {
            val api = BlockingQuestionsApi()
            val vm = viewModel(repo(api = api))

            assertTrue(vm.uiState.value.isLoading)
            assertFalse("submit must be disabled before load", vm.uiState.value.canSubmit)

            vm.submitExam()
            runCurrent()

            assertFalse(vm.uiState.value.isSubmitted)
            assertEquals(0, api.submitCalls)
        }

    @Test
    fun `submit is rejected when the paper failed to load`() =
        runTest {
            val vm = viewModel(repo(api = FailingQuestionsApi()))
            runCurrent()

            assertNotNull(vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.canSubmit)

            vm.submitExam()
            runCurrent()

            assertFalse(vm.uiState.value.isSubmitted)
        }

    @Test
    fun `empty paper becomes an error state with retry available`() =
        runTest {
            val vm = viewModel(repo())
            runCurrent()

            val state = vm.uiState.value
            assertFalse(state.isLoading)
            assertEquals(ExamViewModel.EMPTY_PAPER_MESSAGE, state.errorMessage)
            assertFalse(state.canSubmit)
            assertTrue(state.questions.isEmpty())
        }

    @Test
    fun `retry reloads with forceRefresh and recovers`() =
        runTest {
            val api = FailingQuestionsApi()
            val vm = viewModel(repo(api = api))
            runCurrent()

            assertNotNull(vm.uiState.value.errorMessage)
            assertEquals(1, api.questionCalls)

            vm.retry()
            runCurrent()

            val state = vm.uiState.value
            assertNull("error must clear after a successful retry", state.errorMessage)
            assertEquals(listOf("q1"), state.questions.map { it.id })
            assertTrue(state.canSubmit)
            assertEquals("retry must bypass the cache and hit the network", 2, api.questionCalls)
        }

    @Test
    fun `resumed session starts from the persisted deadline`() =
        runTest {
            val vm = viewModel(repo(), deadline = 30L * 60L * 1000L)
            runCurrent()

            assertEquals(1800L, vm.uiState.value.remainingSeconds)
            assertTrue("a shortened clock must show the resumed notice", vm.uiState.value.isResumed)
        }

    @Test
    fun `fresh session is not flagged as resumed`() =
        runTest {
            val vm = viewModel(repo(), deadline = 90L * 60L * 1000L)
            runCurrent()

            assertEquals(5400L, vm.uiState.value.remainingSeconds)
            assertFalse(vm.uiState.value.isResumed)
        }

    @Test
    fun `startup latency does not turn a fresh exam into a resumed one`() =
        runTest {
            testScheduler.advanceTimeBy(50L)
            val vm = viewModel(repo(), deadline = 90L * 60L * 1000L)
            runCurrent()

            assertEquals("must open at 90:00, not 89:59", 5400L, vm.uiState.value.remainingSeconds)
            assertFalse("fresh exam must not show the resumed banner", vm.uiState.value.isResumed)
        }

    @Test
    fun `a genuinely resumed exam still shows the banner`() =
        runTest {
            testScheduler.advanceTimeBy(10L * 60L * 1000L)
            val vm = viewModel(repo(), deadline = 90L * 60L * 1000L)
            runCurrent()

            assertEquals(4800L, vm.uiState.value.remainingSeconds)
            assertTrue(vm.uiState.value.isResumed)
        }

    @Test
    fun `expired deadline clamps to zero`() =
        runTest {
            val vm = viewModel(repo(), deadline = -60_000L)
            runCurrent()

            assertEquals(0L, vm.uiState.value.remainingSeconds)
        }

    @Test
    fun `timer counts down from the deadline`() =
        runTest {
            val vm = viewModel(repo(), deadline = 90L * 60L * 1000L)
            runCurrent()
            assertEquals(5400L, vm.uiState.value.remainingSeconds)

            advanceTimeBySeconds(30)

            assertEquals(5370L, vm.uiState.value.remainingSeconds)
        }

    @Test
    fun `more than three violations hand the exam in with the count`() =
        runTest {
            val api = PaperExamApi()
            val vm = viewModel(repo(api = api))
            runCurrent()

            vm.recordViolations(3)
            runCurrent()
            assertFalse(vm.uiState.value.isSubmitted)

            vm.recordViolations(4)
            runCurrent()

            assertTrue(vm.uiState.value.isSubmitted)
            assertTrue(vm.uiState.value.violationExit)
            assertEquals(4, api.submits.single().violations)
        }

    @Test
    fun `a repeated merged count does not grow the violation total`() =
        runTest {
            val api = ViolationReportingApi()
            val vm = viewModel(repo(api = api))
            runCurrent()

            vm.recordViolations(2)
            runCurrent()
            vm.recordViolations(2)
            runCurrent()
            vm.recordViolations(2)
            runCurrent()

            assertEquals(listOf(2), api.reported)
            assertFalse(vm.uiState.value.isSubmitted)
        }

    @Test
    fun `each new violation is reported to the server at once`() =
        runTest {
            val api = ViolationReportingApi()
            val vm = viewModel(repo(api = api))
            runCurrent()

            vm.recordViolations(1)
            runCurrent()
            vm.recordViolations(2)
            runCurrent()
            vm.recordViolations(2)
            runCurrent()

            assertEquals(listOf(1, 2), api.reported)
        }

    @Test
    fun `the count the server holds comes back into the state`() =
        runTest {
            val api = ViolationReportingApi(serverCount = 3)
            val vm = viewModel(repo(api = api))
            runCurrent()

            vm.recordViolations(0)
            runCurrent()

            assertEquals(3, vm.uiState.value.serverViolations)
        }

    @Test
    fun `a report that failed is sent again with the next violation`() =
        runTest {
            val api = ViolationReportingApi(failFirst = 1)
            val vm = viewModel(repo(api = api))
            runCurrent()

            vm.recordViolations(1)
            runCurrent()
            assertEquals(emptyList<Int>(), api.reported)

            vm.recordViolations(2)
            runCurrent()

            assertEquals(listOf(2), api.reported)
        }

    @Test
    fun `a normal hand-in still tells the server how often the student left`() =
        runTest {
            val api = PaperExamApi()
            val vm = viewModel(repo(api = api))
            runCurrent()

            vm.recordViolations(2)
            vm.submitExam()
            runCurrent()

            assertTrue(vm.uiState.value.isSubmitted)
            assertFalse(vm.uiState.value.violationExit)
            assertEquals(2, api.submits.single().violations)
        }

    @Test
    fun `a failed forced hand-in keeps trying until it goes through`() =
        runTest {
            val api = FlakySubmitApi(failFirst = 2)
            val vm = viewModel(repo(api = api))
            runCurrent()

            vm.recordViolations(4)
            runCurrent()
            advanceTimeBySeconds(15)

            assertTrue(vm.uiState.value.isSubmitted)
            assertEquals(3, api.submitCalls)
        }

    @Test
    fun `the ui summary counts confirmed and pending rows`() =
        runTest {
            val dao =
                InMemoryAnswerDao(
                    pendingAnswer("q1").copy(
                        syncState = AnswerSyncState.SERVER_CONFIRMED,
                        confirmedAtMillis = 1_000L,
                    ),
                    pendingAnswer("q2"),
                )
            val vm = viewModel(repo(dao = dao))
            runCurrent()

            val summary = vm.uiState.value.sync
            assertEquals(2, summary.totalAnswered)
            assertEquals(1, summary.confirmedCount)
            assertEquals(1, summary.pendingCount)
            assertFalse(summary.allConfirmed)
        }

    private fun TestScope.viewModel(
        repository: ExamRepository,
        sessionId: String = "s1",
        deadline: Long? = null,
    ) = ExamViewModel(
        repository = repository,
        sessionId = sessionId,
        deadline = deadline,
        clock = testClock(testScheduler),
    )

    private fun repo(
        dao: InMemoryAnswerDao = InMemoryAnswerDao(),
        api: ExamApiService = StubExamApi(),
    ) = ExamRepository(api, dao, QuestionDaoStub(), now = { 5_000L })

    private fun TestScope.advanceTimeBySeconds(seconds: Long) {
        testScheduler.advanceTimeBy(seconds * 1000L)
        testScheduler.runCurrent()
    }
}

private class QuestionDaoStub : id.sch.mts241alikhlash.exam.data.local.QuestionDao {
    private var stored: List<id.sch.mts241alikhlash.exam.data.local.CachedQuestionEntity> = emptyList()

    override suspend fun getForSession(sessionId: String) = stored

    override suspend fun insertAll(items: List<id.sch.mts241alikhlash.exam.data.local.CachedQuestionEntity>) {
        stored = items
    }

    override suspend fun deleteForSession(sessionId: String) {
        stored = emptyList()
    }
}

private class BlockingSubmitApi : ExamApiService {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.exam.data.model.SessionHeartbeatResponse =
        id.sch.mts241alikhlash.exam.data.model
            .SessionHeartbeatResponse()

    private val started = CompletableDeferred<Unit>()
    private val release = CompletableDeferred<Unit>()
    var submitCalls = 0
        private set

    suspend fun awaitSubmitStarted() = started.await()

    fun completeSubmit() = release.complete(Unit)

    override suspend fun getQuestions(sessionId: String) = paperOf(listOf(question("q1")))

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchRequest,
    ): SaveAnswersBatchResultDto = batchAck(request)

    override suspend fun submit(
        sessionId: String,
        request: SubmitRequest,
    ): SubmittedSessionDto {
        submitCalls++
        started.complete(Unit)
        release.await()
        return SubmittedSessionDto("SUBMITTED")
    }

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
}

private class FailingApi : ExamApiService {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.exam.data.model.SessionHeartbeatResponse =
        id.sch.mts241alikhlash.exam.data.model
            .SessionHeartbeatResponse()

    override suspend fun getQuestions(sessionId: String) = paperOf(listOf(question("q1")))

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchRequest,
    ): SaveAnswersBatchResultDto = throw IOException("offline")

    override suspend fun submit(
        sessionId: String,
        request: SubmitRequest,
    ): SubmittedSessionDto = throw IOException("offline")

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
}

private class FlakySubmitApi(
    private var failFirst: Int,
) : ExamApiService {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.exam.data.model.SessionHeartbeatResponse =
        id.sch.mts241alikhlash.exam.data.model
            .SessionHeartbeatResponse()

    var submitCalls = 0
        private set

    override suspend fun getQuestions(sessionId: String) = paperOf(listOf(question("q1")))

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchRequest,
    ): SaveAnswersBatchResultDto = batchAck(request)

    override suspend fun submit(
        sessionId: String,
        request: SubmitRequest,
    ): SubmittedSessionDto {
        submitCalls++
        if (failFirst > 0) {
            failFirst--
            throw IOException("offline")
        }
        return SubmittedSessionDto("SUBMITTED")
    }

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
}

private class FailingQuestionsApi : ExamApiService {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.exam.data.model.SessionHeartbeatResponse =
        id.sch.mts241alikhlash.exam.data.model
            .SessionHeartbeatResponse()

    var questionCalls = 0
        private set

    override suspend fun getQuestions(sessionId: String): SessionPaperDto {
        questionCalls++
        if (questionCalls == 1) throw IOException("offline")
        return paperOf(listOf(question("q1")))
    }

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
}

private class BlockingQuestionsApi : ExamApiService {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.exam.data.model.SessionHeartbeatResponse =
        id.sch.mts241alikhlash.exam.data.model
            .SessionHeartbeatResponse()

    private val never = CompletableDeferred<Unit>()
    var submitCalls = 0
        private set

    override suspend fun getQuestions(sessionId: String): SessionPaperDto {
        never.await()
        return paperOf(listOf(question("q1")))
    }

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchRequest,
    ): SaveAnswersBatchResultDto = batchAck(request)

    override suspend fun submit(
        sessionId: String,
        request: SubmitRequest,
    ): SubmittedSessionDto {
        submitCalls++
        return SubmittedSessionDto("SUBMITTED")
    }

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
}

private class ViolationReportingApi(
    private val serverCount: Int = 0,
    private var failFirst: Int = 0,
) : ExamApiService {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.exam.data.model.SessionHeartbeatResponse =
        id.sch.mts241alikhlash.exam.data.model
            .SessionHeartbeatResponse()

    val reported = mutableListOf<Int>()

    override suspend fun getQuestions(sessionId: String) = paperOf(listOf(question("q1")))

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchRequest,
    ): SaveAnswersBatchResultDto = batchAck(request)

    override suspend fun submit(
        sessionId: String,
        request: SubmitRequest,
    ): SubmittedSessionDto = SubmittedSessionDto("SUBMITTED")

    override suspend fun reportViolations(
        sessionId: String,
        request: ViolationReport,
    ): ViolationCountDto {
        if (failFirst > 0) {
            failFirst--
            throw IOException("offline")
        }
        if (request.count > 0) reported += request.count
        return ViolationCountDto(maxOf(serverCount, request.count))
    }

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
}

private fun question(id: String) =
    PaperQuestionDto(
        id = id,
        number = 1,
        type = "MULTIPLE_CHOICE",
        bodyHtml = "<p>Soal $id</p>",
        points = 1.0,
        choices = choices("A", "B"),
    )
