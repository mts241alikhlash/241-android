package id.sch.mts241alikhlash.cbt.ui.screens

import id.sch.mts241alikhlash.cbt.EmptyQuestionDao
import id.sch.mts241alikhlash.cbt.InMemoryAnswerDao
import id.sch.mts241alikhlash.cbt.MainDispatcherRule
import id.sch.mts241alikhlash.cbt.choices
import id.sch.mts241alikhlash.cbt.data.local.AnswerSyncState
import id.sch.mts241alikhlash.cbt.data.model.PaperQuestionDto
import id.sch.mts241alikhlash.cbt.data.model.RecordSessionEventsRequest
import id.sch.mts241alikhlash.cbt.data.model.RecordSessionEventsResponse
import id.sch.mts241alikhlash.cbt.data.model.ResumeSessionRequest
import id.sch.mts241alikhlash.cbt.data.model.ResumedSessionDto
import id.sch.mts241alikhlash.cbt.data.model.SaveAnswersBatchRequest
import id.sch.mts241alikhlash.cbt.data.model.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.cbt.data.model.SavedAnswerDto
import id.sch.mts241alikhlash.cbt.data.model.SessionPaperDto
import id.sch.mts241alikhlash.cbt.data.model.SubmitRequest
import id.sch.mts241alikhlash.cbt.data.model.SubmittedSessionDto
import id.sch.mts241alikhlash.cbt.data.model.ViolationCountDto
import id.sch.mts241alikhlash.cbt.data.model.ViolationReport
import id.sch.mts241alikhlash.cbt.data.remote.ExamApiService
import id.sch.mts241alikhlash.cbt.data.repository.ExamRepository
import id.sch.mts241alikhlash.cbt.pendingAnswer
import id.sch.mts241alikhlash.cbt.testClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExamSubmitGateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class GateApi(
        private val minSubmitMinutes: Int,
        private val submitResult: SubmittedSessionDto = SubmittedSessionDto("SUBMITTED")
    ) : ExamApiService {
        override suspend fun sessionHeartbeat(
            sessionId: String
        ): id.sch.mts241alikhlash.cbt.data.model.SessionHeartbeatResponse =
            id.sch.mts241alikhlash.cbt.data.model.SessionHeartbeatResponse()

        var submitCalls = 0
            private set

        override suspend fun getQuestions(sessionId: String) = SessionPaperDto(
            questions = listOf(
                PaperQuestionDto("q1", 1, "MULTIPLE_CHOICE", "x", 1.0, choices("A", "B"))
            ),
            startedAt = "1970-01-01T00:00:00.000Z",
            minSubmitMinutes = minSubmitMinutes,
            serverTime = "1970-01-01T00:00:00.000Z"
        )

        override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

        override suspend fun saveAnswersBatch(
            sessionId: String,
            request: SaveAnswersBatchRequest
        ): SaveAnswersBatchResultDto = id.sch.mts241alikhlash.cbt.batchAck(request)

        override suspend fun submit(sessionId: String, request: SubmitRequest): SubmittedSessionDto {
            submitCalls++
            return submitResult
        }

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

    private fun TestScope.viewModel(
        api: GateApi,
        dao: InMemoryAnswerDao = InMemoryAnswerDao()
    ) = ExamViewModel(
        repository = ExamRepository(api, dao, EmptyQuestionDao(), now = { testScheduler.currentTime }),
        sessionId = "s1",
        deadline = 60 * 60 * 1000L,
        clock = testClock(testScheduler)
    )

    @Test
    fun `submit is locked before the minimum submit time and shows the remaining minutes`() = runTest {
        val vm = viewModel(GateApi(minSubmitMinutes = 20))
        runCurrent()

        val state = vm.uiState.value

        assertFalse("submit must be locked before the window opens", state.canSubmit)
        assertEquals(20L, state.submitGateMinutes)
        assertEquals(20 * 60L, state.submitGateSeconds)
    }

    @Test
    fun `submit unlocks once the minimum submit time has passed`() = runTest {
        val api = GateApi(minSubmitMinutes = 20)
        val vm = viewModel(api)
        runCurrent()

        testScheduler.advanceTimeBy(20 * 60 * 1000L)
        runCurrent()

        val state = vm.uiState.value
        assertTrue("submit must unlock after 20 minutes", state.canSubmit)
        assertEquals(0L, state.submitGateSeconds)

        vm.submitExam()
        runCurrent()

        assertEquals(1, api.submitCalls)
        assertTrue(vm.uiState.value.isSubmitted)
    }

    @Test
    fun `an expired submit keeps the carried answers pending`() = runTest {
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        val api = GateApi(
            minSubmitMinutes = 0,
            submitResult = SubmittedSessionDto("EXPIRED", answersStored = false)
        )
        val vm = viewModel(api, dao)
        runCurrent()

        vm.submitExam()
        runCurrent()

        assertTrue(vm.uiState.value.isSubmitted)
        assertEquals(AnswerSyncState.LOCAL_PENDING, dao.rows.getValue("q1").syncState)
        assertEquals(1, dao.countPendingAnswers("s1"))
        assertEquals(ExamViewModel.ANSWERS_NOT_STORED_MESSAGE, vm.uiState.value.deliveryWarning)
    }
}
