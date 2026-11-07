package id.sch.mts241alikhlash.exam.ui.screens

import id.sch.mts241alikhlash.exam.kiosk.ViolationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpContentTest {
    @Test
    fun `every topic has a title and at least one point`() {
        (examRules + appGuide).forEach { topic ->
            assertTrue(topic.title, topic.title.isNotBlank())
            assertTrue(topic.title, topic.points.isNotEmpty())
            topic.points.forEach { assertTrue(topic.title, it.isNotBlank()) }
        }
    }

    @Test
    fun `the rules open with the violation limit the app really enforces`() {
        val first = examRules.first()

        assertEquals("YANG PALING PENTING", first.title)
        val text = first.points.joinToString(" ")
        assertTrue(text.contains("${ViolationPolicy.LIMIT} kali"))
        assertTrue(text.contains("ke-${ViolationPolicy.LIMIT + 1}"))
    }

    @Test
    fun `the violation limit is stated in one place only`() {
        val mentions = examRules.flatMap { it.points }.count { it.contains("kali.") && it.contains("Batasnya") }

        assertEquals(1, mentions)
    }

    @Test
    fun `the guide follows the exam from sign-in to hand-in`() {
        assertEquals(
            listOf("MASUK", "MELIHAT UJIAN", "MEMULAI UJIAN", "MENGERJAKAN SOAL", "MENGUMPULKAN", "PROFIL DAN KELUAR"),
            appGuide.map { it.title },
        )
    }
}
