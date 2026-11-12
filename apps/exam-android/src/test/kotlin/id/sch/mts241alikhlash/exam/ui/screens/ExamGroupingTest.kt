package id.sch.mts241alikhlash.exam.ui.screens

import id.sch.mts241alikhlash.exam.api.exam.models.MyExamDto
import id.sch.mts241alikhlash.exam.myExam
import org.junit.Assert.assertEquals
import org.junit.Test

class ExamGroupingTest {
    private fun exam(
        id: String,
        type: String = "ASSIGNMENT",
        category: String = "NON_EXAM",
        activityId: String? = null,
        eventId: String? = null,
        makeup: Boolean = false,
    ) = myExam(
        id = id,
        title = "Ujian $id",
        subjectId = "s1",
        startsAt = "2026-11-01T01:00:00.000Z",
        endsAt = "2026-11-01T03:00:00.000Z",
        durationMinutes = 90,
        status = MyExamDto.Status.OPEN,
        type = MyExamDto.Type.valueOf(type),
        category = MyExamDto.Category.valueOf(category),
        activityId = activityId,
        eventId = eventId,
        makeup = makeup,
    )

    @Test
    fun `exams with category EXAM go to the exam menu and NON_EXAM to the non-exam menu`() {
        val midterm = exam("a", type = "MIDTERM", category = "EXAM", activityId = "act-1")
        val finalExam = exam("b", type = "FINAL", category = "EXAM", activityId = "act-2")
        val daily = exam("c", type = "DAILY", category = "NON_EXAM", activityId = "act-3")
        val assignment = exam("d", type = "ASSIGNMENT", category = "NON_EXAM")
        val all = listOf(midterm, finalExam, daily, assignment)

        assertEquals(listOf(midterm, finalExam), examMenuExams(all, examWork = true))
        assertEquals(listOf(daily, assignment), examMenuExams(all, examWork = false))
    }

    @Test
    fun `a makeup sitting of a DAILY schedule stays in the non-exam menu`() {
        val dailyMakeup = exam("m", type = "DAILY", category = "NON_EXAM", makeup = true)
        val midtermMakeup = exam("n", type = "MIDTERM", category = "EXAM", makeup = true)
        val all = listOf(dailyMakeup, midtermMakeup)

        assertEquals(listOf(midtermMakeup), examMenuExams(all, examWork = true))
        assertEquals(listOf(dailyMakeup), examMenuExams(all, examWork = false))
    }

    @Test
    fun `examTypeLabel maps the four canonical types`() {
        assertEquals("UTS", examTypeLabel("MIDTERM"))
        assertEquals("UAS", examTypeLabel("FINAL"))
        assertEquals("Ulangan Harian", examTypeLabel("DAILY"))
        assertEquals("Tugas", examTypeLabel("ASSIGNMENT"))
    }
}
