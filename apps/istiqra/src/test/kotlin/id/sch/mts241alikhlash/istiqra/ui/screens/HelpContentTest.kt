package id.sch.mts241alikhlash.istiqra.ui.screens

import id.sch.mts241alikhlash.istiqra.api.exam.models.ExamPolicyDto
import id.sch.mts241alikhlash.istiqra.data.session.ExamRuntimePolicy
import id.sch.mts241alikhlash.istiqra.kiosk.ViolationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpContentTest {
    @Test
    fun `every topic has a title and at least one point`() {
        (examRules() + appGuide).forEach { topic ->
            assertTrue(topic.title, topic.title.isNotBlank())
            assertTrue(topic.title, topic.points.isNotEmpty())
            topic.points.forEach { assertTrue(topic.title, it.isNotBlank()) }
        }
    }

    @Test
    fun `the rules open with the violation limit the app really enforces`() {
        val first = examRules().first()

        assertEquals("Yang paling penting", first.title)
        val text = first.points.joinToString(" ")
        assertTrue(text.contains("${ViolationPolicy.LIMIT} kali"))
        assertTrue(text.contains("ke-${ViolationPolicy.LIMIT + 1}"))
    }

    @Test
    fun `the rules state the runtime violation limit`() {
        val previous = ExamRuntimePolicy.current
        ExamRuntimePolicy.apply(ExamPolicyDto(violationLimit = 5))

        try {
            val text = examRules().first().points.joinToString(" ")

            assertTrue(text, text.contains("Batasnya 5 kali"))
            assertTrue(text, text.contains("ke-6"))
        } finally {
            ExamRuntimePolicy.apply(previous)
        }
    }

    @Test
    fun `the violation limit is stated in one place only`() {
        val mentions = examRules().flatMap { it.points }.count { it.contains("kali.") && it.contains("Batasnya") }

        assertEquals(1, mentions)
    }

    @Test
    fun `the guide names both menus`() {
        val text = appGuide.flatMap { it.points }.joinToString(" ")

        assertTrue(text.contains("Menu Ujian"))
        assertTrue(text.contains("menu Non-ujian"))
        assertTrue(text.contains("Ujian dan Non-ujian"))
    }

    @Test
    fun `the guide follows the exam from sign-in to hand-in`() {
        assertEquals(
            listOf(
                "Masuk",
                "Ujian dan jadwal",
                "Memulai",
                "Mengerjakan soal",
                "Mengumpulkan",
                "Profil dan keluar",
            ),
            appGuide.map { it.title },
        )
    }
}
