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
import id.sch.mts241alikhlash.exam.api.exam.models.SubmitSessionRequestDto
import id.sch.mts241alikhlash.exam.api.exam.models.SubmitSessionResponseDto
import id.sch.mts241alikhlash.exam.api.exam.models.ViolationCountDto
import id.sch.mts241alikhlash.exam.batchAck
import id.sch.mts241alikhlash.exam.choices
import id.sch.mts241alikhlash.exam.data.repository.ExamRepository
import id.sch.mts241alikhlash.exam.eventsResponse
import id.sch.mts241alikhlash.exam.heartbeatOf
import id.sch.mts241alikhlash.exam.paperOf
import id.sch.mts241alikhlash.exam.pendingAnswer
import id.sch.mts241alikhlash.exam.submitted
import id.sch.mts241alikhlash.exam.testClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ExamSubmitFlowTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FlowApi(
        var submitFailures: Int = 0,
    ) : StubExamApi() {
        override suspend fun sessionHeartbeat(
            sessionId: String,
        ): id.sch.mts241alikhlash.exam.api.exam.models.SessionHeartbeatDto = heartbeatOf()

        val events = mutableListOf<String>()
        var submitCalls = 0

        override suspend fun getQuestions(sessionId: String) = paperOf(listOf(PaperQuestionDto("q1", 1, PaperQuestionDto.Type.MULTIPLE_CHOICE, "x", 1.0, choices("a", "b"))))

        override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

        override suspend fun saveAnswersBatch(
            sessionId: String,
            request: SaveAnswersBatchDto,
        ): SaveAnswersBatchResultDto {
            events += "batch:${request.answers.size}"
            return batchAck(request)
        }

        override suspend fun submit(
            sessionId: String,
            request: SubmitSessionRequestDto,
        ): SubmitSessionResponseDto {
            events += "submit"
            submitCalls++
            submits += request
            if (submitFailures > 0) {
                submitFailures--
                throw IOException("offline")
            }
            return submitted(SubmitSessionResponseDto.Status.GRADED)
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

    private fun TestScope.newViewModel(
        api: FlowApi,
        dao: InMemoryAnswerDao,
        deadline: Long? = null,
    ) = ExamViewModel(
        repository = ExamRepository(api, dao, EmptyQuestionDao()),
        sessionId = "s1",
        deadline = deadline,
        clock = testClock(testScheduler),
    )

    @Test
    fun `submit carries the pending answers in one trip`() =
        runTest {
            val api = FlowApi()
            val dao = InMemoryAnswerDao(*Array(46) { pendingAnswer("q$it") })
            val vm = newViewModel(api, dao)
            runCurrent()

            vm.submitExam()
            runCurrent()

            assertEquals(listOf("submit"), api.events)
            val request = api.submits.single()
            assertEquals(46, request.pendingAnswers?.size)
            assertEquals(46, request.expectedAnswerCount)
            assertTrue(vm.uiState.value.isSubmitted)
            assertNull(vm.uiState.value.submitError)
        }

    @Test
    fun `submitting twice leaves the screen submitted and posts once`() =
        runTest {
            val api = FlowApi()
            val vm = newViewModel(api, InMemoryAnswerDao())
            runCurrent()

            vm.submitExam()
            runCurrent()
            vm.submitExam()
            runCurrent()

            assertEquals(1, api.submitCalls)
            assertTrue(vm.uiState.value.isSubmitted)
        }

    @Test
    fun `a failed submit keeps the exam open and can be retried`() =
        runTest {
            val api = FlowApi(submitFailures = 1)
            val vm = newViewModel(api, InMemoryAnswerDao())
            runCurrent()

            vm.submitExam()
            runCurrent()
            assertFalse(vm.uiState.value.isSubmitted)
            assertEquals("Tidak dapat terhubung ke server.", vm.uiState.value.submitError)

            vm.submitExam()
            runCurrent()
            assertTrue(vm.uiState.value.isSubmitted)
        }

    @Test
    fun `a short count from the server is retried once after a batch sync`() =
        runTest {
            val api = FlowApi()
            val dao = InMemoryAnswerDao(pendingAnswer("q1"))
            val vm = newViewModel(api, dao)
            runCurrent()

            vm.submitExam()
            runCurrent()

            assertTrue(vm.uiState.value.isSubmitted)
            assertEquals(1, api.submitCalls)
        }

    @Test
    fun `a closed sending window blocks the hand-in and surfaces the server message`() =
        runTest {
            val closed =
                HttpException(
                    Response.error<Any>(
                        409,
                        """{"statusCode":409,"message":"Waktu pengiriman jawaban sudah habis","data":null}""".toResponseBody("application/json".toMediaType()),
                    ),
                )
            val api = FlowApi(submitFailures = 1)
            val dao = InMemoryAnswerDao(pendingAnswer("q1"))
            val vm =
                ExamViewModel(
                    repository =
                        ExamRepository(
                            api = FailingBatchApi(closed),
                            dao = dao,
                            questionDao = EmptyQuestionDao(),
                        ),
                    sessionId = "s1",
                    deadline = 60 * 60 * 1000L,
                    clock = testClock(testScheduler),
                )
            runCurrent()

            vm.submitExam()
            runCurrent()

            assertFalse(vm.uiState.value.isSubmitted)
            assertEquals("Waktu pengiriman jawaban sudah habis", vm.uiState.value.submitError)
        }

    @Test
    fun `time over submits once automatically`() =
        runTest {
            val api = FlowApi()
            val vm = newViewModel(api, InMemoryAnswerDao(), deadline = 2_000L)
            runCurrent()

            advanceTimeBy(3_000L)
            runCurrent()
            advanceTimeBy(20_000L)
            runCurrent()

            assertTrue(vm.uiState.value.isTimeOver)
            assertTrue(vm.uiState.value.isSubmitted)
            assertEquals(1, api.submitCalls)
        }

    @Test
    fun `time over keeps retrying until the submit goes through`() =
        runTest {
            val api = FlowApi(submitFailures = 2)
            val vm = newViewModel(api, InMemoryAnswerDao(), deadline = 1_000L)
            runCurrent()

            advanceTimeBy(30_000L)
            runCurrent()

            assertTrue(vm.uiState.value.isSubmitted)
            assertEquals(3, api.submitCalls)
        }

    @Test
    fun `answers are frozen once time is over`() =
        runTest {
            val api = FlowApi(submitFailures = 100)
            val vm = newViewModel(api, InMemoryAnswerDao(), deadline = 1_000L)
            runCurrent()

            advanceTimeBy(2_000L)
            runCurrent()

            assertFalse(vm.uiState.value.canAnswer)
        }

    @Test
    fun `automatic submit gives up after the grace window and offers a manual retry`() =
        runTest {
            val api = FlowApi(submitFailures = 1000)
            val vm = newViewModel(api, InMemoryAnswerDao(), deadline = 1_000L)
            runCurrent()

            advanceTimeBy(
                ExamViewModel.AUTO_SUBMIT_MAX_ATTEMPTS * ExamViewModel.AUTO_SUBMIT_RETRY_MS + 2_000L,
            )
            runCurrent()

            assertFalse(vm.uiState.value.isSubmitted)
            assertEquals(ExamViewModel.AUTO_SUBMIT_MAX_ATTEMPTS, api.submitCalls)
            assertTrue(vm.uiState.value.canSubmit)
        }

    @Test
    fun `a periodic pass syncs without a keystroke`() =
        runTest {
            val api = FlowApi()
            val dao = InMemoryAnswerDao(pendingAnswer("q1"))
            newViewModel(api, dao)
            runCurrent()

            advanceTimeBy(ExamViewModel.SYNC_INTERVAL_MS + 100)
            runCurrent()

            assertEquals(listOf("batch:1"), api.events)
        }

    @Test
    fun `a connectivity change triggers a debounced sync`() =
        runTest {
            val api = FlowApi()
            val dao = InMemoryAnswerDao(pendingAnswer("q1"))
            val vm = newViewModel(api, dao)
            runCurrent()

            vm.onConnectivityChanged(true)
            advanceTimeBy(ExamViewModel.SYNC_DEBOUNCE_MS - 1)
            runCurrent()
            assertTrue("the sync must wait for the debounce", api.events.isEmpty())

            advanceTimeBy(2)
            runCurrent()
            assertEquals(listOf("batch:1"), api.events)
        }

    @Test
    fun `no sync is attempted while offline`() =
        runTest {
            val api = FlowApi()
            val dao = InMemoryAnswerDao(pendingAnswer("q1"))
            val vm =
                ExamViewModel(
                    repository = ExamRepository(api, dao, EmptyQuestionDao()),
                    sessionId = "s1",
                    deadline = 60 * 60 * 1000L,
                    clock = testClock(testScheduler),
                    isOnline = { false },
                )
            runCurrent()

            advanceTimeBy(ExamViewModel.SYNC_INTERVAL_MS * 3)
            runCurrent()

            assertTrue(api.events.isEmpty())
        }

    private class FailingBatchApi(
        private val failure: Exception,
    ) : StubExamApi() {
        override suspend fun sessionHeartbeat(
            sessionId: String,
        ): id.sch.mts241alikhlash.exam.api.exam.models.SessionHeartbeatDto = heartbeatOf()

        override suspend fun getQuestions(sessionId: String) = paperOf(listOf(PaperQuestionDto("q1", 1, PaperQuestionDto.Type.MULTIPLE_CHOICE, "x", 1.0, choices("a", "b"))))

        override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

        override suspend fun saveAnswersBatch(
            sessionId: String,
            request: SaveAnswersBatchDto,
        ): SaveAnswersBatchResultDto = throw failure

        override suspend fun submit(
            sessionId: String,
            request: SubmitSessionRequestDto,
        ): SubmitSessionResponseDto = throw failure

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
}
