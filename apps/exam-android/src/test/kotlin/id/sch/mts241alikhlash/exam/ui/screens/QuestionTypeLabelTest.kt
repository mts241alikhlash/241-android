package id.sch.mts241alikhlash.exam.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class QuestionTypeLabelTest {
    @Test
    fun `maps known types`() {
        assertEquals("Pilihan Ganda", questionTypeLabel("multiple_choice"))
        assertEquals("Benar / Salah", questionTypeLabel("true_false"))
        assertEquals("Uraian", questionTypeLabel("essay"))
        assertEquals("Jawaban Singkat", questionTypeLabel("short_answer"))
    }

    @Test
    fun `unknown type falls back to readable sentence case`() {
        assertEquals("Matching pairs", questionTypeLabel("matching_pairs"))
    }

    @Test
    fun `blank type returns generic label`() {
        assertEquals("Soal", questionTypeLabel("   "))
    }

    @Test
    fun `mapping is independent of the default locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("Pilihan Ganda", questionTypeLabel("multiple_choice"))
            assertEquals("Benar / Salah", questionTypeLabel("true_false"))
            assertEquals("Jawaban Singkat", questionTypeLabel("short_answer"))
            assertEquals("Matching pairs", questionTypeLabel("matching_pairs"))
            assertEquals("Pilihan Ganda", questionTypeLabel("MULTIPLE_CHOICE"))
        } finally {
            Locale.setDefault(original)
        }
    }
}

class QuestionTypeLabelCoverageTest {
    @org.junit.Test
    fun covers_all_five_server_types() {
        org.junit.Assert.assertEquals("Pilihan Ganda", questionTypeLabel("MULTIPLE_CHOICE"))
        org.junit.Assert.assertEquals("Pilihan Ganda Kompleks", questionTypeLabel("MULTIPLE_RESPONSE"))
        org.junit.Assert.assertEquals("Benar / Salah", questionTypeLabel("TRUE_FALSE"))
        org.junit.Assert.assertEquals("Jawaban Singkat", questionTypeLabel("SHORT_ANSWER"))
        org.junit.Assert.assertEquals("Uraian", questionTypeLabel("ESSAY"))
    }
}
