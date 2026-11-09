package id.sch.mts241alikhlash.exam.ui.screens

import id.sch.mts241alikhlash.exam.data.model.ExamSummaryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamGroupingTest {
    private fun exam(
        id: String,
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
        activityId = activityId,
        eventId = eventId,
        makeup = makeup,
    )

    @Test
    fun `activity event and makeup schedules are exam work`() {
        assertTrue(isExamWork(exam("a", activityId = "act-1")))
        assertTrue(isExamWork(exam("b", eventId = "evt-1")))
        assertTrue(isExamWork(exam("c", makeup = true)))
    }

    @Test
    fun `a plain assignment without an activity is not exam work`() {
        assertFalse(isExamWork(exam("d")))
    }

    @Test
    fun `the menu split keeps each list separate`() {
        val activity = exam("a", activityId = "act-1")
        val event = exam("b", eventId = "evt-1")
        val makeup = exam("c", makeup = true)
        val task = exam("d")
        val all = listOf(activity, task, event, makeup)

        assertEquals(listOf(activity, event, makeup), examMenuExams(all, examWork = true))
        assertEquals(listOf(task), examMenuExams(all, examWork = false))
    }
}
