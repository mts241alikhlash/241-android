package id.sch.mts241alikhlash.exam.ui.screens

import id.sch.mts241alikhlash.exam.api.exam.models.PaperQuestionDto
import id.sch.mts241alikhlash.exam.data.model.AnswerPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmitSummaryTest {
    private fun question(id: String) = PaperQuestionDto(id, id.removePrefix("q").toInt(), PaperQuestionDto.Type.MULTIPLE_CHOICE, "x", 1.0, null)

    @Test
    fun `counts answered doubtful unanswered and pending sync`() {
        val state =
            ExamUiState(
                isLoading = false,
                questions = listOf(question("q1"), question("q2"), question("q3"), question("q4")),
                answers = mapOf("q1" to AnswerPayload.Choice("A")),
                drafts = mapOf("q2" to "jawaban"),
                doubtfuls = setOf("q2", "q3"),
                sync = AnswerSyncSummary(totalAnswered = 3, confirmedCount = 0, pendingCount = 3),
            )

        val summary = submitSummary(state)

        assertEquals(4, summary.total)
        assertEquals(2, summary.answered)
        assertEquals(2, summary.doubtful)
        assertEquals(2, summary.unanswered)
        assertEquals(3, summary.pendingSync)
    }

    @Test
    fun `blank draft is not answered`() {
        val state =
            ExamUiState(
                isLoading = false,
                questions = listOf(question("q1")),
                drafts = mapOf("q1" to "   "),
            )

        val summary = submitSummary(state)

        assertEquals(0, summary.answered)
        assertEquals(1, summary.unanswered)
    }
}
