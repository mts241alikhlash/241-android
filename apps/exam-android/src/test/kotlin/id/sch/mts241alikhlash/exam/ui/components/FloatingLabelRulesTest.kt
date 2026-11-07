package id.sch.mts241alikhlash.exam.ui.components

import id.sch.mts241alikhlash.exam.ui.theme.WebTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingLabelRulesTest {
    @Test
    fun `label floats when focused even if empty`() {
        assertTrue(labelFloated(true, "", null))
    }

    @Test
    fun `label floats when it has a value`() {
        assertTrue(labelFloated(false, "a", null))
    }

    @Test
    fun `label rests when empty and unfocused`() {
        assertFalse(labelFloated(false, "", null))
    }

    @Test
    fun `error forces the label to float`() {
        assertTrue(labelFloated(false, "", "Wajib diisi"))
    }

    @Test
    fun `error text replaces the label`() {
        assertEquals("Wajib diisi", labelText("NIS", "Wajib diisi"))
        assertEquals("NIS", labelText("NIS", null))
    }

    @Test
    fun `label color follows state`() {
        assertEquals(WebTokens.Destructive, labelColor(false, "x"))
        assertEquals(WebTokens.Primary, labelColor(true, null))
        assertEquals(WebTokens.Foreground.copy(alpha = 0.7f), labelColor(false, null))
    }

    @Test
    fun `border color follows state`() {
        assertEquals(WebTokens.Destructive, borderColor(false, "x"))
        assertEquals(WebTokens.Primary, borderColor(true, null))
        assertEquals(WebTokens.InputBorder, borderColor(false, null))
    }

    @Test
    fun `error beats focus`() {
        assertEquals(WebTokens.Destructive, borderColor(true, "x"))
    }
}
