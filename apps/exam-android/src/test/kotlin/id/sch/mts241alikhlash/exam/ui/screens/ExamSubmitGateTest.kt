package id.sch.mts241alikhlash.exam.ui.screens

import id.sch.mts241alikhlash.exam.EmptyQuestionDao
import id.sch.mts241alikhlash.exam.InMemoryAnswerDao
import id.sch.mts241alikhlash.exam.MainDispatcherRule
import id.sch.mts241alikhlash.exam.StubExamApi
import id.sch.mts241alikhlash.exam.api.exam.models.PaperQuestionDto
import id.sch.mts241alikhlash.exam.api.exam.models.RecordSessionEventsRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.RecordSessionEventsResponseDto
import id.sch.mts241alikhlash.exam.api.exam.models.RecordViolationsRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.ResumeSessionRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.ResumeSessionResponseDto
import id.sch.mts241alikhlash.exam.api.exam.models.SaveAnswersBatchDto
import id.sch.mts241alikhlash.exam.api.exam.models.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.exam.api.exam.models.SavedAnswerDto
import id.sch.mts241alikhlash.exam.api.exam.models.SessionPaperDto
import id.sch.mts241alikhlash.exam.api.exam.models.SubmitSessionRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.SubmitSessionResponseDto
import id.sch.mts241alikhlash.exam.api.exam.models.ViolationCountDto
import id.sch.mts241alikhlash.exam.choices
import id.sch.mts241alikhlash.exam.data.local.AnswerSyncState
import id.sch.mts241alikhlash.exam.data.repository.ExamRepository
import id.sch.mts241alikhlash.exam.eventsResponse
import id.sch.mts241alikhlash.exam.heartbeatOf
import id.sch.mts241alikhlash.exam.pendingAnswer
import id.sch.mts241alikhlash.exam.submitted
import id.sch.mts241alikhlash.exam.testClock
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
        private val submitResult: SubmitSessionResponseDto = submitted(SubmitSessionResponseDto.Status.SUBMITTED),
    ) : StubExamApi() {
        override suspend fun sessionHeartbeat(
            sessionId: String,
        ): id.sch.mts241alikhlash.exam.api.exam.models.SessionHeartbeatDto = heartbeatOf()

        var submitCalls = 0
            private set

        override suspend fun getQuestions(sessionId: String) =
            SessionPaperDto(
                questions =
                    listOf(
                        PaperQuestionDto("q1", 1, PaperQuestionDto.Type.MULTIPLE_CHOICE, "x", 1.0, choices("A", "B")),
                    ),
                startedAt = "1970-01-01T00:00:00.000Z",
                minSubmitMinutes = minSubmitMinutes,
                serverTime = "1970-01-01T00:00:00.000Z",
            )

        override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

        override suspend fun saveAnswersBatch(
            sessionId: String,
            request: SaveAnswersBatchDto,
        ): SaveAnswersBatchResultDto =
            id.sch.mts241alikhlash.exam
                .batchAck(request)

        override suspend fun submit(
            sessionId: String,
            request: SubmitSessionRequestDto,
        ): SubmitSessionResponseDto {
            submitCalls++
            return submitResult
        }

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

    private fun TestScope.viewModel(
        api: GateApi,
        dao: InMemoryAnswerDao = InMemoryAnswerDao(),
    ) = ExamViewModel(
        repository = ExamRepository(api, dao, EmptyQuestionDao(), now = { testScheduler.currentTime }),
        sessionId = "s1",
        deadline = 60 * 60 * 1000L,
        clock = testClock(testScheduler),
    )

    @Test
    fun `submit is locked before the minimum submit time and shows the remaining minutes`() =
        runTest {
            val vm = viewModel(GateApi(minSubmitMinutes = 20))
            runCurrent()

            val state = vm.uiState.value

            assertFalse("submit must be locked before the window opens", state.canSubmit)
            assertEquals(20L, state.submitGateMinutes)
            assertEquals(20 * 60L, state.submitGateSeconds)
        }

    @Test
    fun `submit unlocks once the minimum submit time has passed`() =
        runTest {
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
    fun `an expired submit keeps the carried answers pending`() =
        runTest {
            val dao = InMemoryAnswerDao(pendingAnswer("q1"))
            val api =
                GateApi(
                    minSubmitMinutes = 0,
                    submitResult = submitted(SubmitSessionResponseDto.Status.EXPIRED, answersStored = false),
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
