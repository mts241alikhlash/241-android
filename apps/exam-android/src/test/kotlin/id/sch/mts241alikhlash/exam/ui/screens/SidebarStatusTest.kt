package id.sch.mts241alikhlash.exam.ui.screens

import id.sch.mts241alikhlash.exam.data.model.ExamSummaryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SidebarStatusTest {
    private fun exam(
        title: String,
        status: String,
        startsAt: String = "2026-10-30T01:00:00.000Z",
    ) = ExamSummaryDto(
        id = title,
        title = title,
        subjectId = "s",
        startsAt = startsAt,
        endsAt = "2026-10-30T03:00:00.000Z",
        durationMinutes = 90,
        status = status,
    )

    @Test
    fun `says there is no exam when the list is empty or only finished`() {
        assertEquals("Tidak ada ujian", examStatusSummary(emptyList()))
        assertEquals("Tidak ada ujian", examStatusSummary(listOf(exam("UAS - IPA", "FINISHED"))))
    }

    @Test
    fun `an exam in progress wins over everything else`() {
        val summary =
            examStatusSummary(
                listOf(exam("UH - IPS", "OPEN"), exam("UTS - IPA", "IN_PROGRESS"), exam("UAS", "UPCOMING")),
            )

        assertEquals("Ujian berlangsung · UTS - IPA", summary)
    }

    @Test
    fun `one open exam is named and several are counted`() {
        assertEquals("Ujian dibuka · UH - IPS", examStatusSummary(listOf(exam("UH - IPS", "OPEN"))))
        assertEquals(
            "2 ujian sedang dibuka",
            examStatusSummary(listOf(exam("UH - IPS", "OPEN"), exam("UH - IPA", "OPEN"))),
        )
    }

    @Test
    fun `the earliest upcoming exam is shown when nothing is open`() {
        val summary =
            examStatusSummary(
                listOf(
                    exam("UAS - IPA", "UPCOMING", "2026-11-05T01:00:00.000Z"),
                    exam("UTS - IPS", "UPCOMING", "2026-11-02T01:00:00.000Z"),
                ),
            )

        assertEquals("Berikutnya · UTS - IPS", summary)
    }

    @Test
    fun `the answer sync label is honest about what the server holds`() {
        assertEquals(
            "Tersimpan: 45 soal (Terkirim ke server: 45)",
            answerSyncLabel(AnswerSyncSummary(totalAnswered = 45, confirmedCount = 45, pendingCount = 0)),
        )
        assertEquals(
            "Tersimpan di HP: 46 soal (Menunggu koneksi: 4)",
            answerSyncLabel(AnswerSyncSummary(totalAnswered = 46, confirmedCount = 42, pendingCount = 4)),
        )
    }

    @Test
    fun `semester and academic year join, and are absent when both are`() {
        assertEquals("Ganjil · 2026/2027", semesterLabel("Ganjil", "2026/2027"))
        assertEquals("Ganjil", semesterLabel("Ganjil", null))
        assertEquals("2026/2027", semesterLabel(null, "2026/2027"))
        assertNull(semesterLabel(null, null))
    }
}
