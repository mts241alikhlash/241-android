package id.sch.mts241alikhlash.exam.ui.screens

import id.sch.mts241alikhlash.exam.EmptyQuestionDao
import id.sch.mts241alikhlash.exam.InMemoryAnswerDao
import id.sch.mts241alikhlash.exam.MainDispatcherRule
import id.sch.mts241alikhlash.exam.StubExamApi
import id.sch.mts241alikhlash.exam.data.model.SUSPENSION_REASON_CALL
import id.sch.mts241alikhlash.exam.data.model.SuspensionRequest
import id.sch.mts241alikhlash.exam.data.model.SuspensionResponseDto
import id.sch.mts241alikhlash.exam.data.remote.ApiException
import id.sch.mts241alikhlash.exam.data.repository.ExamRepository
import id.sch.mts241alikhlash.exam.data.session.ExamTimer
import id.sch.mts241alikhlash.exam.testClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExamSuspensionTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun TestScope.viewModel(
        api: StubExamApi,
        extended: MutableList<Long> = mutableListOf(),
    ): ExamViewModel {
        api.suspensionDeadline = EXTENDED_DEADLINE
        return ExamViewModel(
            repository = ExamRepository(api, InMemoryAnswerDao(), EmptyQuestionDao(), now = { testScheduler.currentTime }),
            sessionId = "s1",
            deadline = ExamTimer.EXAM_DURATION_MS,
            clock = testClock(testScheduler),
            onDeadlineExtended = { extended += it },
        )
    }

    @Test
    fun `a call pauses the exam and its length is reported when the call ends`() =
        runTest {
            val api = StubExamApi()
            val extended = mutableListOf<Long>()
            val vm = viewModel(api, extended)
            runCurrent()

            vm.onCallOrMicChanged(active = true, nowMs = 1_000)
            testScheduler.advanceTimeBy(91_000)
            vm.onCallOrMicChanged(active = false, nowMs = 92_000)
            runCurrent()

            val request = api.suspensions.single()
            assertEquals(SUSPENSION_REASON_CALL, request.reason)
            assertEquals(91, request.seconds)
            assertTrue(request.clientId.isNotBlank())
            assertEquals(listOf(5_640_000L), extended)
        }

    @Test
    fun `a failed report is retried with the same client id`() =
        runTest {
            val api = StubExamApi()
            api.suspensionFailures = 1
            val vm = viewModel(api)
            runCurrent()

            vm.onCallOrMicChanged(active = true, nowMs = 0)
            vm.onCallOrMicChanged(active = false, nowMs = 30_000)
            runCurrent()

            assertEquals(1, api.suspensions.size)
            testScheduler.advanceTimeBy(2_000)
            runCurrent()

            assertEquals(2, api.suspensions.size)
            assertEquals(30, api.suspensions.first().seconds)
            assertEquals(
                api.suspensions.last().clientId,
                api.suspensions.first().clientId,
            )
        }

    @Test
    fun `a closed session is not retried`() =
        runTest {
            val api =
                object : StubExamApi() {
                    override suspend fun reportSuspension(
                        sessionId: String,
                        request: SuspensionRequest,
                    ): SuspensionResponseDto {
                        suspensions += request
                        throw ApiException(409, "Sesi ujian tidak ditemukan")
                    }
                }
            val vm = viewModel(api)
            runCurrent()

            vm.onCallOrMicChanged(active = true, nowMs = 0)
            vm.onCallOrMicChanged(active = false, nowMs = 60_000)
            runCurrent()
            testScheduler.advanceTimeBy(60_000)
            runCurrent()

            assertEquals(1, api.suspensions.size)
        }

    private companion object {
        const val EXTENDED_DEADLINE = "1970-01-01T01:34:00Z"
    }
}
