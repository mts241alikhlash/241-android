package id.sch.mts241alikhlash.exam.ui.exam

sealed interface HtmlSpan {
    data class Text(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false,
        val subscript: Boolean = false,
        val superscript: Boolean = false,
    ) : HtmlSpan

    data object LineBreak : HtmlSpan
}

sealed interface HtmlBlock {
    data class Paragraph(
        val spans: List<HtmlSpan>,
        val rtl: Boolean,
    ) : HtmlBlock

    data class ListBlock(
        val ordered: Boolean,
        val items: List<List<HtmlSpan>>,
    ) : HtmlBlock

    data class Table(
        val rows: List<List<List<HtmlSpan>>>,
    ) : HtmlBlock

    data class Image(
        val fileId: String,
    ) : HtmlBlock
}

fun List<HtmlSpan>.plain(): String =
    joinToString("") {
        when (it) {
            is HtmlSpan.Text -> it.text
            HtmlSpan.LineBreak -> "\n"
        }
    }

fun parseHtml(html: String): List<HtmlBlock> =
    try {
        HtmlParser(html).parse()
    } catch (_: RuntimeException) {
        listOf(HtmlBlock.Paragraph(listOf(HtmlSpan.Text(html.replace(TAGS, " ").trim())), rtl = false))
    }

private val TAGS = Regex("<[^>]*>")
private val WHITESPACE = Regex("[ \t\r\n]+")
private val ENTITY = Regex("&(#[xX][0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);")
private val FILE_ID = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
private val BLOCK_TAGS = setOf("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "pre")

private class Tag(
    val name: String,
    val closing: Boolean,
    val attributes: Map<String, String>,
)

private class HtmlParser(
    private val html: String,
) {
    private val blocks = mutableListOf<HtmlBlock>()
    private val spans = mutableListOf<HtmlSpan>()
    private var bold = 0
    private var italic = 0
    private var underline = 0
    private var sub = 0
    private var sup = 0

    private var listOrdered: Boolean? = null
    private val listItems = mutableListOf<List<HtmlSpan>>()
    private var itemOpen = false

    private var tableOpen = false
    private val tableRows = mutableListOf<List<List<HtmlSpan>>>()
    private val rowCells = mutableListOf<List<HtmlSpan>>()
    private var rowOpen = false
    private var cellOpen = false
    private val pendingImages = mutableListOf<String>()

    fun parse(): List<HtmlBlock> {
        var index = 0
        val text = StringBuilder()
        while (index < html.length) {
            val char = html[index]
            if (char == '<' && index + 1 < html.length && (html[index + 1].isLetter() || html[index + 1] == '/' || html[index + 1] == '!')) {
                if (html.startsWith("<!--", index)) {
                    pushText(text)
                    val end = html.indexOf("-->", index + 4)
                    index = if (end < 0) html.length else end + 3
                    continue
                }
                val end = tagEnd(index)
                if (end < 0) {
                    text.append(html, index, html.length)
                    break
                }
                pushText(text)
                val tag = readTag(html.substring(index + 1, end))
                index = end + 1
                if (tag != null) {
                    if (!tag.closing && (tag.name == "script" || tag.name == "style")) {
                        val close = html.indexOf("</${tag.name}", index, ignoreCase = true)
                        index = if (close < 0) html.length else (html.indexOf('>', close).takeIf { it >= 0 } ?: (html.length - 1)) + 1
                    } else {
                        handle(tag)
                    }
                }
            } else {
                text.append(char)
                index++
            }
        }
        pushText(text)
        closeAll()
        return blocks
    }

    private fun tagEnd(start: Int): Int {
        var quote: Char? = null
        var i = start + 1
        while (i < html.length) {
            val c = html[i]
            when {
                quote != null -> if (c == quote) quote = null
                c == '"' || c == '\'' -> quote = c
                c == '>' -> return i
            }
            i++
        }
        return -1
    }

    private fun readTag(inner: String): Tag? {
        var body = inner.trim()
        val closing = body.startsWith("/")
        if (closing) body = body.removePrefix("/").trim()
        if (body.startsWith("!")) return null
        body = body.removeSuffix("/").trim()
        val name = body.takeWhile { !it.isWhitespace() }.lowercase()
        if (name.isEmpty() || !name[0].isLetter()) return null
        val attributes = mutableMapOf<String, String>()
        Regex("""([a-zA-Z_:][-a-zA-Z0-9_:.]*)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+))""")
            .findAll(body.substring(name.length))
            .forEach { match ->
                val value = match.groupValues[2].ifEmpty { match.groupValues[3].ifEmpty { match.groupValues[4] } }
                attributes[match.groupValues[1].lowercase()] = value
            }
        return Tag(name, closing, attributes)
    }

    private fun pushText(buffer: StringBuilder) {
        if (buffer.isEmpty()) return
        val decoded = decode(buffer.toString()).replace(WHITESPACE, " ")
        buffer.clear()
        if (decoded.isEmpty()) return
        spans += HtmlSpan.Text(decoded, bold > 0, italic > 0, underline > 0, sub > 0, sup > 0)
    }

    private fun handle(tag: Tag) {
        val open = !tag.closing
        when (tag.name) {
            "strong", "b" -> bold = (bold + if (open) 1 else -1).coerceAtLeast(0)
            "em", "i" -> italic = (italic + if (open) 1 else -1).coerceAtLeast(0)
            "u" -> underline = (underline + if (open) 1 else -1).coerceAtLeast(0)
            "sub" -> sub = (sub + if (open) 1 else -1).coerceAtLeast(0)
            "sup" -> sup = (sup + if (open) 1 else -1).coerceAtLeast(0)
            "br" -> if (open) spans += HtmlSpan.LineBreak
            "img" -> if (open) image(tag)
            in BLOCK_TAGS ->
                if (!itemOpen && !cellOpen) {
                    flushParagraph()
                } else if (!open) {
                    spans += HtmlSpan.LineBreak
                }
            "ul", "ol" -> if (open) startList(tag.name == "ol") else closeList()
            "li" -> if (open) startItem() else closeItem()
            "table" -> if (open) startTable() else closeTable()
            "tr" -> if (open) startRow() else closeRow()
            "td", "th" -> if (open) startCell() else closeCell()
        }
    }

    private fun image(tag: Tag) {
        val id = tag.attributes["data-file-id"]?.takeIf { FILE_ID.matches(it) } ?: return
        if (itemOpen || cellOpen) {
            pendingImages += id.lowercase()
            return
        }
        flushParagraph()
        blocks += HtmlBlock.Image(id.lowercase())
    }

    private fun flushParagraph() {
        val cleaned = normalize(spans)
        spans.clear()
        if (cleaned.isNotEmpty()) blocks += HtmlBlock.Paragraph(cleaned, isMostlyArabic(cleaned.plain()))
    }

    private fun startList(ordered: Boolean) {
        closeList()
        flushParagraph()
        listOrdered = ordered
    }

    private fun startItem() {
        closeItem()
        if (listOrdered == null) listOrdered = false
        spans.clear()
        itemOpen = true
    }

    private fun closeItem() {
        if (!itemOpen) return
        val cleaned = normalize(spans)
        spans.clear()
        if (cleaned.isNotEmpty()) listItems += cleaned
        itemOpen = false
    }

    private fun closeList() {
        closeItem()
        val ordered = listOrdered
        if (ordered != null && listItems.isNotEmpty()) blocks += HtmlBlock.ListBlock(ordered, listItems.toList())
        listItems.clear()
        listOrdered = null
        emitPendingImages()
    }

    private fun startTable() {
        closeTable()
        flushParagraph()
        tableOpen = true
    }

    private fun startRow() {
        if (!tableOpen) startTable()
        closeRow()
        rowOpen = true
    }

    private fun startCell() {
        if (!rowOpen) startRow()
        closeCell()
        spans.clear()
        cellOpen = true
    }

    private fun closeCell() {
        if (!cellOpen) return
        rowCells += normalize(spans)
        spans.clear()
        cellOpen = false
    }

    private fun closeRow() {
        closeCell()
        if (rowOpen && rowCells.isNotEmpty()) tableRows += rowCells.toList()
        rowCells.clear()
        rowOpen = false
    }

    private fun closeTable() {
        closeRow()
        if (tableOpen && tableRows.isNotEmpty()) blocks += HtmlBlock.Table(tableRows.toList())
        tableRows.clear()
        tableOpen = false
        emitPendingImages()
    }

    private fun emitPendingImages() {
        pendingImages.forEach { blocks += HtmlBlock.Image(it) }
        pendingImages.clear()
    }

    private fun closeAll() {
        closeList()
        closeTable()
        flushParagraph()
    }
}

private fun normalize(input: List<HtmlSpan>): List<HtmlSpan> {
    val merged = mutableListOf<HtmlSpan>()
    for (span in input) {
        val last = merged.lastOrNull()
        if (span is HtmlSpan.Text && last is HtmlSpan.Text && last.sameStyle(span)) {
            merged[merged.lastIndex] = last.copy(text = last.text + span.text)
        } else {
            merged += span
        }
    }
    val trimmed = merged.toMutableList()
    for (i in trimmed.indices) {
        val span = trimmed[i] as? HtmlSpan.Text ?: continue
        var text = span.text
        val before = trimmed.getOrNull(i - 1)
        val after = trimmed.getOrNull(i + 1)
        if (before == null || before is HtmlSpan.LineBreak) text = text.trimStart(' ')
        if (after == null || after is HtmlSpan.LineBreak) text = text.trimEnd(' ')
        trimmed[i] = span.copy(text = text)
    }
    val cleaned = trimmed.filterNot { it is HtmlSpan.Text && it.text.isEmpty() }.toMutableList()
    while (cleaned.firstOrNull() is HtmlSpan.LineBreak) cleaned.removeAt(0)
    while (cleaned.lastOrNull() is HtmlSpan.LineBreak) cleaned.removeAt(cleaned.lastIndex)
    return cleaned
}

private fun HtmlSpan.Text.sameStyle(other: HtmlSpan.Text) =
    bold == other.bold &&
        italic == other.italic &&
        underline == other.underline &&
        subscript == other.subscript &&
        superscript == other.superscript

private fun decode(text: String): String =
    if (!text.contains('&')) {
        text
    } else {
        ENTITY.replace(text) { match ->
            val body = match.groupValues[1]
            when {
                body.startsWith("#x") || body.startsWith("#X") -> codePoint(body.drop(2).toIntOrNull(16), match.value)
                body.startsWith("#") -> codePoint(body.drop(1).toIntOrNull(), match.value)
                else ->
                    when (body.lowercase()) {
                        "amp" -> "&"
                        "lt" -> "<"
                        "gt" -> ">"
                        "quot" -> "\""
                        "apos" -> "'"
                        "nbsp" -> " "
                        else -> match.value
                    }
            }
        }
    }

private fun codePoint(
    value: Int?,
    original: String,
): String = if (value != null && Character.isValidCodePoint(value) && value != 0) String(Character.toChars(value)) else original

private fun isMostlyArabic(text: String): Boolean {
    var letters = 0
    var arabic = 0
    for (char in text) {
        if (!char.isLetter()) continue
        letters++
        val code = char.code
        if (code in 0x0600..0x06FF || code in 0x0750..0x077F || code in 0x08A0..0x08FF || code in 0xFB50..0xFDFF || code in 0xFE70..0xFEFF) arabic++
    }
    return letters > 0 && arabic * 2 >= letters
}
