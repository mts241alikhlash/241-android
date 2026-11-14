package id.sch.mts241alikhlash.istiqra.ui.screens

import id.sch.mts241alikhlash.istiqra.InMemoryAnswerDao
import id.sch.mts241alikhlash.istiqra.MainDispatcherRule
import id.sch.mts241alikhlash.istiqra.StubExamApi
import id.sch.mts241alikhlash.istiqra.api.exam.models.PaperQuestionDto
import id.sch.mts241alikhlash.istiqra.data.local.CachedAnswerEntity
import id.sch.mts241alikhlash.istiqra.data.local.CachedQuestionEntity
import id.sch.mts241alikhlash.istiqra.data.local.QuestionDao
import id.sch.mts241alikhlash.istiqra.data.model.AnswerPayload
import id.sch.mts241alikhlash.istiqra.data.repository.ExamRepository
import id.sch.mts241alikhlash.istiqra.paperOf
import id.sch.mts241alikhlash.istiqra.testClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExamViewModelTextDraftTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class SlowAnswerDao : InMemoryAnswerDao() {
        override fun observeAnswers(sessionId: String): Flow<List<CachedAnswerEntity>> = super.observeAnswers(sessionId).onEach { delay(EMIT_LAG_MS) }
    }

    private class NoQuestions : QuestionDao {
        override suspend fun getForSession(sessionId: String) = emptyList<CachedQuestionEntity>()

        override suspend fun insertAll(items: List<CachedQuestionEntity>) = Unit

        override suspend fun deleteForSession(sessionId: String) = Unit
    }

    private class EssayApi : StubExamApi() {
        override suspend fun getQuestions(sessionId: String) =
            paperOf(
                listOf(
                    PaperQuestionDto(id = "e1", number = 1, type = PaperQuestionDto.Type.ESSAY, bodyHtml = "Jelaskan", points = 5.0, choices = emptyList()),
                ),
            )
    }

    private fun ExamUiState.shownText(questionId: String) = drafts[questionId] ?: (answers[questionId] as? AnswerPayload.Text)?.text.orEmpty()

    @Test
    fun `typed text stays on screen while the saved answer catches up`() =
        runTest {
            val vm =
                ExamViewModel(
                    repository = ExamRepository(EssayApi(), SlowAnswerDao(), NoQuestions(), now = { 5_000L }),
                    sessionId = "s1",
                    deadline = null,
                    clock = testClock(testScheduler),
                )
            testScheduler.advanceTimeBy(EMIT_LAG_MS * 2)
            runCurrent()

            vm.updateText("e1", "Fotosintesis")
            val shown = mutableListOf<String>()
            repeat(40) {
                testScheduler.advanceTimeBy(50)
                runCurrent()
                shown += vm.uiState.value.shownText("e1")
            }

            assertEquals(listOf("Fotosintesis"), shown.distinct())
        }

    private companion object {
        const val EMIT_LAG_MS = 200L
    }
}
