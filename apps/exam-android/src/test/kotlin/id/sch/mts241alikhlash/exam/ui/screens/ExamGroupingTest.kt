package id.sch.mts241alikhlash.exam.ui.screens

import id.sch.mts241alikhlash.exam.data.model.ExamSummaryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamGroupingTest {
    private fun exam(
        id: String,
        type: String = "ASSIGNMENT",
        activityId: String? = null,
        eventId: String? = null,
        makeup: Boolean = false,
    ) = ExamSummaryDto(
        id = id,
        title = "Ujian $id",
        subjectId = "s1",
        startsAt = "2026-11-01T01:00:00.000Z",
        endsAt = "2026-11-01T03:00:00.000Z",
        durationMinutes = 90,
        status = "OPEN",
        type = type,
        activityId = activityId,
        eventId = eventId,
        makeup = makeup,
    )

    @Test
    fun `uts uas and makeups are exam work`() {
        assertTrue(isExamWork(exam("a", type = "UTS")))
        assertTrue(isExamWork(exam("b", type = "UAS")))
        assertTrue(isExamWork(exam("c", makeup = true)))
    }

    @Test
    fun `assignments quizzes and daily tests stay out of the exam menu even when they belong to an activity`() {
        assertFalse(isExamWork(exam("d", type = "ASSIGNMENT", activityId = "act-1", eventId = "evt-1")))
        assertFalse(isExamWork(exam("e", type = "QUIZ", activityId = "act-1")))
        assertFalse(isExamWork(exam("f", type = "UH", eventId = "evt-1")))
    }

    @Test
    fun `the menu split keeps each list separate`() {
        val uts = exam("a", type = "UTS", activityId = "act-1")
        val task = exam("d", type = "ASSIGNMENT", activityId = "act-2")
        val makeup = exam("c", type = "UAS", makeup = true)
        val all = listOf(uts, task, makeup)

        assertEquals(listOf(uts, makeup), examMenuExams(all, examWork = true))
        assertEquals(listOf(task), examMenuExams(all, examWork = false))
    }
}
