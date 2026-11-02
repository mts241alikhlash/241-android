package id.sch.mts241alikhlash.cbt.ui.screens

import id.sch.mts241alikhlash.cbt.data.model.ExamSummaryDto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExamTitleAndWindowTest {

    private fun exam(type: String?, subject: String?) = ExamSummaryDto(
        "e1", "Bilangan dan Operasi Hitung", "s1", "2026-11-02T00:30:00.000Z", "2026-11-02T02:15:00.000Z",
        90, "UPCOMING", subjectName = subject, type = type
    )

    @Test
    fun `title joins the type label and the subject`() {
        assertEquals("UTS - IPA", exam("UTS", "IPA").displayTitle())
        assertEquals("Quiz - Bahasa Inggris", exam("QUIZ", "Bahasa Inggris").displayTitle())
        assertEquals("Tugas - PAI", exam("ASSIGNMENT", "PAI").displayTitle())
    }

    @Test
    fun `title falls back to the package title without a type or a subject`() {
        assertEquals("Bilangan dan Operasi Hitung", exam(null, "IPA").displayTitle())
        assertEquals("Bilangan dan Operasi Hitung", exam("UTS", null).displayTitle())
        assertEquals("Bilangan dan Operasi Hitung", exam("UTS", "  ").displayTitle())
        assertEquals("Bilangan dan Operasi Hitung", exam("OTHER", "IPA").displayTitle())
    }

    @Test
    fun `window formats the date and clock in the given zone`() {
        val window = examWindow("2026-11-02T00:30:00.000Z", "2026-11-02T02:15:00.000Z", TimeZone.getTimeZone("UTC"))!!

        assertEquals("Senin, 2 November 2026", window.date)
        assertEquals("00.30", window.start)
        assertEquals("02.15", window.end)
    }

    @Test
    fun `window follows the zone across midnight`() {
        val window = examWindow("2026-11-02T20:00:00.000Z", "2026-11-02T21:45:00.000Z", TimeZone.getTimeZone("Asia/Jakarta"))!!

        assertEquals("Selasa, 3 November 2026", window.date)
        assertEquals("03.00", window.start)
        assertEquals("04.45", window.end)
    }

    @Test
    fun `window is null for an unreadable timestamp`() {
        assertNull(examWindow("soon", "later"))
    }

    private fun at(iso: String): Long =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.ROOT).parse(iso)!!.time

    @Test
    fun `opens-in reads in minutes, hours and days`() {
        val start = "2026-11-02T00:00:00.000Z"

        assertEquals("kurang dari 1 menit", examOpensIn(start, at("2026-11-01T23:59:30.000Z")))
        assertEquals("45 menit", examOpensIn(start, at("2026-11-01T23:15:00.000Z")))
        assertEquals("2 jam", examOpensIn(start, at("2026-11-01T22:00:00.000Z")))
        assertEquals("2 jam 30 menit", examOpensIn(start, at("2026-11-01T21:30:00.000Z")))
        assertEquals("3 hari", examOpensIn(start, at("2026-10-30T00:00:00.000Z")))
        assertEquals("3 hari 5 jam", examOpensIn(start, at("2026-10-29T19:00:00.000Z")))
    }

    @Test
    fun `opens-in is null for an unreadable timestamp`() {
        assertNull(examOpensIn("soon", 0L))
    }

    @Test
    fun `type label covers the five types only`() {
        assertEquals("UH", examTypeLabel("UH"))
        assertEquals("Quiz", examTypeLabel("QUIZ"))
        assertEquals("Tugas", examTypeLabel("ASSIGNMENT"))
        assertEquals("UTS", examTypeLabel("UTS"))
        assertEquals("UAS", examTypeLabel("UAS"))
        assertNull(examTypeLabel("PTS"))
        assertNull(examTypeLabel(null))
    }
}
