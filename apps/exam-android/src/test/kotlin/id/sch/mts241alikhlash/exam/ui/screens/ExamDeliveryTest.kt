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
import id.sch.mts241alikhlash.exam.data.model.AnswerPayload
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
import kotlinx.coroutines.test.advanceUntilIdle
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

@OptIn(ExperimentalCoroutinesApi::class)
class ExamDeliveryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun http(
        code: Int,
        message: String,
    ) = HttpException(
        Response.error<Any>(
            code,
            """{"statusCode":$code,"message":"$message","data":null}""".toResponseBody("application/json".toMediaType()),
        ),
    )

    private class Api(
        var batchError: Exception? = null,
        var submitError: Exception? = null,
    ) : StubExamApi() {
        override suspend fun sessionHeartbeat(
            sessionId: String,
        ): id.sch.mts241alikhlash.exam.api.exam.models.SessionHeartbeatDto = heartbeatOf()

        var submitCalls = 0
        var batchCalls = 0

        override suspend fun getQuestions(sessionId: String) = paperOf(listOf(PaperQuestionDto("q1", 1, PaperQuestionDto.Type.MULTIPLE_CHOICE, "x", 1.0, choices("a", "b"))))

        override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

        override suspend fun saveAnswersBatch(
            sessionId: String,
            request: SaveAnswersBatchDto,
        ): SaveAnswersBatchResultDto {
            batchCalls++
            batchError?.let { throw it }
            return batchAck(request)
        }

        override suspend fun submit(
            sessionId: String,
            request: SubmitSessionRequestDto,
        ): SubmitSessionResponseDto {
            submitCalls++
            submitError?.let { throw it }
            return submitted(SubmitSessionResponseDto.Status.EXPIRED)
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
        api: Api,
        dao: InMemoryAnswerDao = InMemoryAnswerDao(),
        deadline: Long = 60 * 60 * 1000L,
        confirmed: MutableList<String> = mutableListOf(),
    ) = ExamViewModel(
        onSubmitConfirmed = { confirmed += it },
        repository = ExamRepository(api, dao, EmptyQuestionDao(), now = { testScheduler.currentTime }),
        sessionId = "s1",
        deadline = deadline,
        clock = testClock(testScheduler),
    )

    private val window = "Waktu pengiriman jawaban sudah habis"
    private val released = "Sesi dilepas. Mulai ujian lagi"

    @Test
    fun `a closed sending window on a sync shows a warning and keeps the exam`() =
        runTest {
            val vm = newViewModel(Api(batchError = http(409, window)))
            runCurrent()

            vm.selectAnswer("q1", AnswerPayload.Choice("A"))
            advanceTimeBy(ExamViewModel.SYNC_DEBOUNCE_MS + 100)
            runCurrent()

            assertEquals(window, vm.uiState.value.deliveryWarning)
            assertNull(vm.uiState.value.lostMessage)
        }

    @Test
    fun `a released session on a sync sends the student out of the exam`() =
        runTest {
            val vm = newViewModel(Api(batchError = http(409, released)))
            runCurrent()

            vm.selectAnswer("q1", AnswerPayload.Choice("A"))
            advanceTimeBy(ExamViewModel.SYNC_DEBOUNCE_MS + 100)
            runCurrent()

            assertEquals(released, vm.uiState.value.lostMessage)
        }

    @Test
    fun `a rejected batch shows the server message`() =
        runTest {
            val vm = newViewModel(Api(batchError = http(400, "Bentuk jawaban tidak sesuai dengan jenis soal")))
            runCurrent()

            vm.selectAnswer("q1", AnswerPayload.Choice("A"))
            advanceTimeBy(ExamViewModel.SYNC_DEBOUNCE_MS + 100)
            runCurrent()

            assertEquals("Bentuk jawaban tidak sesuai dengan jenis soal", vm.uiState.value.deliveryWarning)
        }

    @Test
    fun `submit reports how many answers never arrived`() =
        runTest {
            val dao = InMemoryAnswerDao(pendingAnswer("q1"))
            val vm = newViewModel(Api(submitError = http(409, window)), dao)
            runCurrent()

            vm.submitExam()
            runCurrent()

            assertFalse(vm.uiState.value.isSubmitted)
            assertEquals(1, vm.uiState.value.unsentAnswers)
            assertEquals(window, vm.uiState.value.submitError)
        }

    @Test
    fun `submit of a released session ends the exam instead of retrying`() =
        runTest {
            val api = Api(submitError = http(409, released))
            val vm = newViewModel(api)
            runCurrent()

            vm.submitExam()
            runCurrent()

            assertEquals(released, vm.uiState.value.lostMessage)
            assertFalse(vm.uiState.value.isSubmitted)
            assertEquals(1, api.submitCalls)
        }

    @Test
    fun `time over stops the automatic submit once the session is lost`() =
        runTest {
            val api = Api(submitError = http(409, released))
            val vm = newViewModel(api, deadline = 1_000L)
            runCurrent()

            advanceUntilIdle()

            assertEquals(released, vm.uiState.value.lostMessage)
            assertEquals(1, api.submitCalls)
        }

    @Test
    fun `the local session is cleared as soon as the server confirms the submit`() =
        runTest {
            val confirmed = mutableListOf<String>()
            val vm = newViewModel(Api(), confirmed = confirmed)
            runCurrent()

            vm.submitExam()
            runCurrent()

            assertTrue(vm.uiState.value.isSubmitted)
            assertEquals(listOf("s1"), confirmed)
        }

    @Test
    fun `the local session stays when the submit fails`() =
        runTest {
            val confirmed = mutableListOf<String>()
            val vm = newViewModel(Api(submitError = java.io.IOException("offline")), confirmed = confirmed)
            runCurrent()

            vm.submitExam()
            runCurrent()

            assertTrue(confirmed.isEmpty())
        }
}
