package id.sch.mts241alikhlash.istiqra.ui.exam

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import id.sch.mts241alikhlash.istiqra.ui.theme.ExamTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HtmlContentTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_5,
            theme = "android:Theme.Material.NoActionBar",
        )

    @Test
    fun `renders question html with fractions roots sets and currency cleanly`() {
        val html =
            """
            <p>[Uraian Lengkap] Buktikan secara singkat bahwa untuk <span data-latex="a = \frac{3}{2}">3/2</span> dan <span data-latex="b = \sqrt{12}">sqrt(12)</span>, nilai <span data-latex="\frac{a^2 + b^2}{\sqrt{9}}">frac</span> memenuhi <span data-latex="x \geq 4\frac{1}{4}">4 1/4</span>, serta <span data-latex="x_{1,2} = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}">abc</span>.</p>
            <p>Suhu berubah dari <span data-latex="13^\circ\text{C}">13C</span> menjadi <span data-latex="\sqrt[3]{-27} = -3">root3</span> dan <span data-latex="\sqrt{\frac{9}{4}}">3/2</span> dan harga Rp $500.</p>
            """.trimIndent()

        val blocks = parseHtml(html)
        val renderedPlain =
            blocks
                .filterIsInstance<HtmlBlock.Paragraph>()
                .joinToString("\n") { it.spans.toAnnotated().text }
        assertFalse(renderedPlain.contains("sqrt(12)"))
        assertFalse(renderedPlain.contains("root3"))
        assertTrue(renderedPlain.contains("$500"))
        assertTrue(renderedPlain.contains("13°C"))

        paparazzi.snapshot {
            ExamTheme {
                HtmlContent(
                    html = html,
                    sessionId = "s-test",
                    baseUrl = "http://localhost:5181/",
                    imageLoader = null,
                )
            }
        }
    }
}
