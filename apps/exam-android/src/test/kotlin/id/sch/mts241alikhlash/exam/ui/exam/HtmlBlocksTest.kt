package id.sch.mts241alikhlash.exam.ui.exam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlBlocksTest {
    private val id = "8a1f3c52-1b2d-4e6f-9a7b-0c1d2e3f4a5b"

    @Test
    fun parses_paragraphs_with_inline_styles() {
        val blocks = parseHtml("<p>Satu <strong>tebal</strong> dan <em>miring</em></p><p>Dua</p>")

        assertEquals(2, blocks.size)
        val first = blocks[0] as HtmlBlock.Paragraph
        assertEquals("Satu tebal dan miring", first.spans.plain())
        val bold = first.spans.filterIsInstance<HtmlSpan.Text>().first { it.text == "tebal" }
        assertTrue(bold.bold)
        assertFalse(bold.italic)
        assertEquals("Dua", (blocks[1] as HtmlBlock.Paragraph).spans.plain())
    }

    @Test
    fun parses_lists_and_tables() {
        val blocks = parseHtml("<ol><li>a</li><li>b</li></ol><table><tr><th>H1</th><th>H2</th></tr><tr><td>1</td><td>2</td></tr></table>")

        val list = blocks[0] as HtmlBlock.ListBlock
        assertTrue(list.ordered)
        assertEquals(listOf("a", "b"), list.items.map { it.plain() })
        val table = blocks[1] as HtmlBlock.Table
        assertEquals(listOf(listOf("H1", "H2"), listOf("1", "2")), table.rows.map { row -> row.map { it.plain() } })
    }

    @Test
    fun line_breaks_become_spans() {
        val paragraph = parseHtml("<p>a<br>b</p>").single() as HtmlBlock.Paragraph
        assertEquals(listOf(true), paragraph.spans.filterIsInstance<HtmlSpan.LineBreak>().map { true })
        assertEquals("a\nb", paragraph.spans.plain())
    }

    @Test
    fun sub_and_superscript_are_kept() {
        val paragraph = parseHtml("<p>H<sub>2</sub>O x<sup>2</sup></p>").single() as HtmlBlock.Paragraph
        val texts = paragraph.spans.filterIsInstance<HtmlSpan.Text>()
        assertTrue(texts.first { it.text == "2" && it.subscript }.subscript)
        assertTrue(texts.any { it.text == "2" && it.superscript })
    }

    @Test
    fun keeps_text_inside_unknown_tags() {
        val paragraph = parseHtml("<p>x <blink>tetap</blink> <span style='a'>tampil</span></p>").single() as HtmlBlock.Paragraph
        assertEquals("x tetap tampil", paragraph.spans.plain())
    }

    @Test
    fun drops_script_and_style_content() {
        val blocks = parseHtml("<p>a</p><script>alert(1)</script><style>p{}</style><p>b</p>")
        assertEquals(listOf("a", "b"), blocks.map { (it as HtmlBlock.Paragraph).spans.plain() })
    }

    @Test
    fun accepts_only_uuid_file_ids() {
        val blocks = parseHtml("""<p>a<img data-file-id="$id" alt="peta">b<img data-file-id="../../etc"><img src="http://x/y.png"></p>""")

        val images = blocks.filterIsInstance<HtmlBlock.Image>()
        assertEquals(listOf(id), images.map { it.fileId })
        assertEquals(listOf("a", "b"), blocks.filterIsInstance<HtmlBlock.Paragraph>().map { it.spans.plain() })
    }

    @Test
    fun image_between_paragraphs_keeps_order() {
        val blocks = parseHtml("""<p>atas</p><p><img data-file-id="$id"></p><p>bawah</p>""")
        assertEquals(listOf("P", "I", "P"), blocks.map { if (it is HtmlBlock.Image) "I" else "P" })
    }

    @Test
    fun decodes_entities() {
        val paragraph = parseHtml("<p>5 &lt; 7 &amp; 9 &gt; 3 &quot;q&quot; &#39;s&#39; &#x41;&#66;</p>").single() as HtmlBlock.Paragraph
        assertEquals("5 < 7 & 9 > 3 \"q\" 's' AB", paragraph.spans.plain())
    }

    @Test
    fun a_less_than_sign_in_text_is_text() {
        val paragraph = parseHtml("<p>1 < 2 dan 3 <4</p>").single() as HtmlBlock.Paragraph
        assertEquals("1 < 2 dan 3 <4", paragraph.spans.plain())
    }

    @Test
    fun plain_text_without_tags_is_one_paragraph() {
        assertEquals("halo dunia", (parseHtml("halo   dunia").single() as HtmlBlock.Paragraph).spans.plain())
    }

    @Test
    fun never_throws_on_broken_html() {
        val inputs = listOf("<p", "<p><b>x", "</p></p>", "<table><td>a", "<ul><li>x<ol><li>y", "<img", "<!-- x", "&#99999999999;", "<<>>", "", "   ")
        inputs.forEach { parseHtml(it) }
    }

    @Test
    fun detects_an_arabic_paragraph() {
        val arabic = parseHtml("<p>ما هو الجواب الصحيح؟</p><p>Pilih jawaban</p>")
        assertTrue((arabic[0] as HtmlBlock.Paragraph).rtl)
        assertFalse((arabic[1] as HtmlBlock.Paragraph).rtl)
    }

    @Test
    fun an_image_inside_a_table_cell_follows_the_table() {
        val blocks = parseHtml("""<table><tr><td>a<img data-file-id="$id"></td></tr></table>""")
        assertEquals(listOf("T", "I"), blocks.map { if (it is HtmlBlock.Image) "I" else "T" })
        assertEquals(id, (blocks[1] as HtmlBlock.Image).fileId)
    }

    @Test
    fun an_image_inside_a_list_item_follows_the_list() {
        val blocks = parseHtml("""<ul><li>satu<img data-file-id="$id"></li><li>dua</li></ul>""")
        assertEquals(listOf("L", "I"), blocks.map { if (it is HtmlBlock.Image) "I" else "L" })
        assertEquals(listOf("satu", "dua"), (blocks[0] as HtmlBlock.ListBlock).items.map { it.plain() })
    }
}
