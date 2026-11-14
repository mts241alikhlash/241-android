package id.sch.mts241alikhlash.istiqra.ui.screens

import id.sch.mts241alikhlash.istiqra.EmptyQuestionDao
import id.sch.mts241alikhlash.istiqra.InMemoryAnswerDao
import id.sch.mts241alikhlash.istiqra.MainDispatcherRule
import id.sch.mts241alikhlash.istiqra.StubExamApi
import id.sch.mts241alikhlash.istiqra.api.exam.models.PaperQuestionDto
import id.sch.mts241alikhlash.istiqra.choices
import id.sch.mts241alikhlash.istiqra.data.local.AnswerSyncState
import id.sch.mts241alikhlash.istiqra.data.local.CachedAnswerEntity
import id.sch.mts241alikhlash.istiqra.data.model.AnswerPayload
import id.sch.mts241alikhlash.istiqra.data.repository.ExamRepository
import id.sch.mts241alikhlash.istiqra.paperOf
import id.sch.mts241alikhlash.istiqra.testClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExamAnswerInputTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class RecordingApi : StubExamApi() {
        override suspend fun getQuestions(sessionId: String) =
            paperOf(
                listOf(
                    PaperQuestionDto("q1", 1, PaperQuestionDto.Type.MULTIPLE_CHOICE, "x", 1.0, choices("a", "b", "c")),
                    PaperQuestionDto("q2", 2, PaperQuestionDto.Type.ESSAY, "y", 5.0, null),
                ),
            )
    }

    private fun TestScope.newViewModel(
        api: RecordingApi = RecordingApi(),
        dao: InMemoryAnswerDao = InMemoryAnswerDao(),
    ) = ExamViewModel(
        repository = ExamRepository(api, dao, EmptyQuestionDao(), now = { testScheduler.currentTime }),
        sessionId = "s1",
        deadline = 60 * 60 * 1000L,
        clock = testClock(testScheduler),
    )

    private fun InMemoryAnswerDao.answerOf(questionId: String): AnswerPayload? = rows[questionId]?.let { AnswerPayload.decode(it.answer) }

    @Test
    fun `single choice writes its key locally`() =
        runTest {
            val dao = InMemoryAnswerDao()
            val vm = newViewModel(dao = dao)
            runCurrent()

            vm.selectAnswer("q1", AnswerPayload.Choice("B"))
            runCurrent()

            assertEquals(AnswerPayload.Choice("B"), dao.answerOf("q1"))
            assertEquals(AnswerPayload.Choice("B"), vm.uiState.value.answers["q1"])
            assertEquals(AnswerSyncState.LOCAL_PENDING, dao.rows.getValue("q1").syncState)
        }

    @Test
    fun `true false writes a bool`() =
        runTest {
            val dao = InMemoryAnswerDao()
            val vm = newViewModel(dao = dao)
            runCurrent()

            vm.selectAnswer("q1", AnswerPayload.Bool(false))
            runCurrent()

            assertEquals(AnswerPayload.Bool(false), dao.answerOf("q1"))
        }

    @Test
    fun `multiple response toggles keys and keeps them sorted`() =
        runTest {
            val dao = InMemoryAnswerDao()
            val vm = newViewModel(dao = dao)
            runCurrent()

            vm.toggleKey("q1", "C")
            runCurrent()
            vm.toggleKey("q1", "A")
            runCurrent()
            assertEquals(AnswerPayload.Multiple(listOf("A", "C")), dao.answerOf("q1"))

            vm.toggleKey("q1", "A")
            runCurrent()
            assertEquals(AnswerPayload.Multiple(listOf("C")), dao.answerOf("q1"))
        }

    @Test
    fun `text is written after the debounce`() =
        runTest {
            val dao = InMemoryAnswerDao()
            val vm = newViewModel(dao = dao)
            runCurrent()

            vm.updateText("q2", "jawab")
            advanceTimeBy(ExamViewModel.TEXT_DEBOUNCE_MS - 1)
            runCurrent()
            assertTrue(dao.rows.isEmpty())
            assertEquals("jawab", vm.uiState.value.drafts["q2"])

            advanceTimeBy(2)
            runCurrent()
            assertEquals(AnswerPayload.Text("jawab"), dao.answerOf("q2"))
        }

    @Test
    fun `only the last typed text is stored`() =
        runTest {
            val dao = InMemoryAnswerDao()
            val vm = newViewModel(dao = dao)
            runCurrent()

            vm.updateText("q2", "a")
            advanceTimeBy(100)
            vm.updateText("q2", "ab")
            advanceTimeBy(ExamViewModel.TEXT_DEBOUNCE_MS + 10)
            runCurrent()

            assertEquals(AnswerPayload.Text("ab"), dao.answerOf("q2"))
        }

    @Test
    fun `leaving the question saves the pending text at once`() =
        runTest {
            val dao = InMemoryAnswerDao()
            val vm = newViewModel(dao = dao)
            runCurrent()
            vm.goToIndex(1)

            vm.updateText("q2", "belum selesai")
            vm.goToIndex(0)
            runCurrent()

            assertEquals(AnswerPayload.Text("belum selesai"), dao.answerOf("q2"))
        }

    @Test
    fun `submit saves the pending text first`() =
        runTest {
            val dao = InMemoryAnswerDao()
            val vm = newViewModel(dao = dao)
            runCurrent()

            vm.updateText("q2", "akhir")
            vm.submitExam()
            runCurrent()

            assertEquals(AnswerPayload.Text("akhir"), dao.answerOf("q2"))
            assertTrue(vm.uiState.value.isSubmitted)
        }

    @Test
    fun `text longer than the server limit is cut`() =
        runTest {
            val vm = newViewModel()
            runCurrent()

            var typed = ""
            while (typed.length < 25_000) {
                typed += "x".repeat(ExamViewModel.MAX_TEXT_INSERT)
                vm.updateText("q2", typed)
                typed = vm.uiState.value.drafts["q2"] ?: typed
                if (typed.length >= ExamViewModel.MAX_TEXT_LENGTH) break
            }

            assertEquals(
                ExamViewModel.MAX_TEXT_LENGTH,
                vm.uiState.value.drafts["q2"]
                    ?.length,
            )
        }

    @Test
    fun `marking a question doubtful without an answer keeps the mark`() =
        runTest {
            val dao = InMemoryAnswerDao()
            val vm = newViewModel(dao = dao)
            runCurrent()

            vm.toggleDoubtful("q1")
            runCurrent()

            assertTrue("q1" in vm.uiState.value.doubtfuls)
            assertEquals(true, dao.rows.getValue("q1").isDoubtful)
        }

    @Test
    fun `a doubt mark followed at once by a choice keeps the mark`() =
        runTest {
            val dao = StaleDao()
            val vm =
                ExamViewModel(
                    repository = ExamRepository(RecordingApi(), dao, EmptyQuestionDao(), now = { testScheduler.currentTime }),
                    sessionId = "s1",
                    deadline = 60 * 60 * 1000L,
                    clock = testClock(testScheduler),
                )
            runCurrent()

            vm.toggleDoubtful("q1")
            vm.selectAnswer("q1", AnswerPayload.Choice("A"))
            runCurrent()

            assertTrue(dao.rows.getValue("q1").isDoubtful)
        }

    @Test
    fun `two quick doubt toggles end with the mark off`() =
        runTest {
            val dao = StaleDao()
            val vm =
                ExamViewModel(
                    repository = ExamRepository(RecordingApi(), dao, EmptyQuestionDao(), now = { testScheduler.currentTime }),
                    sessionId = "s1",
                    deadline = 60 * 60 * 1000L,
                    clock = testClock(testScheduler),
                )
            runCurrent()

            vm.toggleDoubtful("q1")
            runCurrent()
            vm.toggleDoubtful("q1")
            runCurrent()

            assertTrue(!dao.rows.getValue("q1").isDoubtful)
        }
}

private class StaleDao : InMemoryAnswerDao() {
    override fun observeAnswers(sessionId: String): Flow<List<CachedAnswerEntity>> = flowOf(emptyList())
}
