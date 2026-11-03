package id.sch.mts241alikhlash.cbt.ui.screens

import id.sch.mts241alikhlash.cbt.EmptyQuestionDao
import id.sch.mts241alikhlash.cbt.InMemoryAnswerDao
import id.sch.mts241alikhlash.cbt.MainDispatcherRule
import id.sch.mts241alikhlash.cbt.batchAck
import id.sch.mts241alikhlash.cbt.paperOf
import id.sch.mts241alikhlash.cbt.choices
import id.sch.mts241alikhlash.cbt.data.model.AnswerPayload
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
import id.sch.mts241alikhlash.cbt.data.repository.ExamRepository
import id.sch.mts241alikhlash.cbt.pendingAnswer
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
import id.sch.mts241alikhlash.cbt.testClock
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ExamSubmitFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FlowApi(
        var submitFailures: Int = 0
    ) : ExamApiService {
        val events = mutableListOf<String>()
        var submitCalls = 0
        val submits = mutableListOf<SubmitRequest>()

        override suspend fun getQuestions(sessionId: String) =
            paperOf(listOf(PaperQuestionDto("q1", 1, "MULTIPLE_CHOICE", "x", 1.0, choices("a", "b"))))

        override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

        override suspend fun saveAnswersBatch(
            sessionId: String,
            request: SaveAnswersBatchRequest
        ): SaveAnswersBatchResultDto {
            events += "batch:${request.answers.size}"
            return batchAck(request)
        }

        override suspend fun submit(sessionId: String, request: SubmitRequest): SubmittedSessionDto {
            events += "submit"
            submitCalls++
            submits += request
            if (submitFailures > 0) {
                submitFailures--
                throw IOException("offline")
            }
            return SubmittedSessionDto("GRADED")
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

    private fun TestScope.newViewModel(api: FlowApi, dao: InMemoryAnswerDao, deadline: Long? = null) =
        ExamViewModel(
            repository = ExamRepository(api, dao, EmptyQuestionDao()),
            sessionId = "s1",
            deadline = deadline,
            clock = testClock(testScheduler)
        )

    @Test
    fun `submit carries the pending answers in one trip`() = runTest {
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
    fun `submitting twice leaves the screen submitted and posts once`() = runTest {
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
    fun `a failed submit keeps the exam open and can be retried`() = runTest {
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
    fun `a short count from the server is retried once after a batch sync`() = runTest {
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
    fun `a closed sending window blocks the hand-in and surfaces the server message`() = runTest {
        val closed = HttpException(
            Response.error<Any>(
                409,
                """{"statusCode":409,"message":"Waktu pengiriman jawaban sudah habis","data":null}""".toResponseBody("application/json".toMediaType())
            )
        )
        val api = FlowApi(submitFailures = 1)
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        val vm = ExamViewModel(
            repository = ExamRepository(
                api = FailingBatchApi(closed),
                dao = dao,
                questionDao = EmptyQuestionDao()
            ),
            sessionId = "s1",
            deadline = 60 * 60 * 1000L,
            clock = testClock(testScheduler)
        )
        runCurrent()

        vm.submitExam()
        runCurrent()

        assertFalse(vm.uiState.value.isSubmitted)
        assertEquals("Waktu pengiriman jawaban sudah habis", vm.uiState.value.submitError)
    }

    @Test
    fun `time over submits once automatically`() = runTest {
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
    fun `time over keeps retrying until the submit goes through`() = runTest {
        val api = FlowApi(submitFailures = 2)
        val vm = newViewModel(api, InMemoryAnswerDao(), deadline = 1_000L)
        runCurrent()

        advanceTimeBy(30_000L)
        runCurrent()

        assertTrue(vm.uiState.value.isSubmitted)
        assertEquals(3, api.submitCalls)
    }

    @Test
    fun `answers are frozen once time is over`() = runTest {
        val api = FlowApi(submitFailures = 100)
        val vm = newViewModel(api, InMemoryAnswerDao(), deadline = 1_000L)
        runCurrent()

        advanceTimeBy(2_000L)
        runCurrent()

        assertFalse(vm.uiState.value.canAnswer)
    }

    @Test
    fun `automatic submit gives up after the grace window and offers a manual retry`() = runTest {
        val api = FlowApi(submitFailures = 1000)
        val vm = newViewModel(api, InMemoryAnswerDao(), deadline = 1_000L)
        runCurrent()

        advanceTimeBy(
            ExamViewModel.AUTO_SUBMIT_MAX_ATTEMPTS * ExamViewModel.AUTO_SUBMIT_RETRY_MS + 2_000L
        )
        runCurrent()

        assertFalse(vm.uiState.value.isSubmitted)
        assertEquals(ExamViewModel.AUTO_SUBMIT_MAX_ATTEMPTS, api.submitCalls)
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `a periodic pass syncs without a keystroke`() = runTest {
        val api = FlowApi()
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        newViewModel(api, dao)
        runCurrent()

        advanceTimeBy(ExamViewModel.SYNC_INTERVAL_MS + 100)
        runCurrent()

        assertEquals(listOf("batch:1"), api.events)
    }

    @Test
    fun `a connectivity change triggers a debounced sync`() = runTest {
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
    fun `no sync is attempted while offline`() = runTest {
        val api = FlowApi()
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        val vm = ExamViewModel(
            repository = ExamRepository(api, dao, EmptyQuestionDao()),
            sessionId = "s1",
            deadline = 60 * 60 * 1000L,
            clock = testClock(testScheduler),
            isOnline = { false }
        )
        runCurrent()

        advanceTimeBy(ExamViewModel.SYNC_INTERVAL_MS * 3)
        runCurrent()

        assertTrue(api.events.isEmpty())
    }

    private class FailingBatchApi(private val failure: Exception) : ExamApiService {
        override suspend fun getQuestions(sessionId: String) =
            paperOf(listOf(PaperQuestionDto("q1", 1, "MULTIPLE_CHOICE", "x", 1.0, choices("a", "b"))))

        override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

        override suspend fun saveAnswersBatch(
            sessionId: String,
            request: SaveAnswersBatchRequest
        ): SaveAnswersBatchResultDto = throw failure

        override suspend fun submit(sessionId: String, request: SubmitRequest): SubmittedSessionDto =
            throw failure

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
}
