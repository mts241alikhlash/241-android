package id.sch.mts241alikhlash.istiqra.ui.exam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatexMathTest {
    @Test
    fun parses_exponents_degrees_and_units() {
        val node = parseLatex("""13^\circ\text{C}""")
        assertFalse(node.hasBlockLayout())
        assertEquals("13°C", node.toPlainText())

        val power = parseLatex("""(-2x^2y^3)^3""")
        assertEquals("(-2x^2y^3)^3", power.toPlainText())
    }

    @Test
    fun parses_mixed_fractions_and_operators() {
        val node = parseLatex("""2\frac{1}{4} \times \frac{2}{3} : 1\frac{1}{2}""")
        assertTrue(node.hasBlockLayout())
        assertEquals("2(1)/(4) × (2)/(3) : 1(1)/(2)", node.toPlainText())
    }

    @Test
    fun parses_roots_with_and_without_index() {
        val plainRoot = parseLatex("""\sqrt{72} + \sqrt{18}""")
        assertTrue(plainRoot.hasBlockLayout())
        assertEquals("√(72) + √(18)", plainRoot.toPlainText())

        val cubeRoot = parseLatex("""\sqrt[3]{27}""")
        assertTrue(cubeRoot.hasBlockLayout())
        assertEquals("√[3](27)", cubeRoot.toPlainText())
    }

    @Test
    fun maps_blackboard_bold_arrows_and_percent() {
        val fn = parseLatex("""f : A \to \mathbb{R}""")
        assertFalse(fn.hasBlockLayout())
        assertEquals("f : A → ℝ", fn.toPlainText())

        val pct = parseLatex("""55\%""")
        assertEquals("55%", pct.toPlainText())
    }

    @Test
    fun maps_set_notation_and_nested_root_over_fraction() {
        val setExpr = parseLatex("""2 \in \{x \mid x \text{ bilangan prima}\}""")
        assertFalse(setExpr.hasBlockLayout())
        val setText = setExpr.toPlainText()
        assertTrue(setText.contains("2 ∈"))
        assertTrue(setText.contains("∣"))
        assertTrue(setText.contains("bilangan prima}"))

        val nested = parseLatex("""\sqrt{\frac{9}{4}}""")
        assertTrue(nested.hasBlockLayout())
        assertEquals("√((9)/(4))", nested.toPlainText())
    }
}
