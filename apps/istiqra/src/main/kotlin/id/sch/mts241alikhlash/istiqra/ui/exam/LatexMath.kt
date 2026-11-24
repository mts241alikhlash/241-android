package id.sch.mts241alikhlash.istiqra.ui.exam

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.istiqra.ui.theme.DMSansFontFamily
import kotlin.math.ceil
import kotlin.math.max

sealed interface MathNode {
    data class Row(
        val items: List<MathNode>,
    ) : MathNode

    data class Text(
        val value: String,
        val italic: Boolean = false,
    ) : MathNode

    data class Script(
        val base: MathNode?,
        val sup: MathNode?,
        val sub: MathNode?,
    ) : MathNode

    data class Fraction(
        val num: MathNode,
        val den: MathNode,
    ) : MathNode

    data class Root(
        val radicand: MathNode,
        val index: MathNode? = null,
    ) : MathNode

    data class Overline(
        val base: MathNode,
    ) : MathNode
}

fun parseLatex(input: String): MathNode = LatexParser(input.trim()).parseAll()

fun MathNode.toPlainText(): String =
    when (this) {
        is MathNode.Row -> items.joinToString("") { it.toPlainText() }
        is MathNode.Text -> value
        is MathNode.Script -> {
            val baseText = base?.toPlainText().orEmpty()
            val subText = sub?.toPlainText()?.let { "_$it" }.orEmpty()
            val supText = sup?.toPlainText()?.let { "^$it" }.orEmpty()
            baseText + subText + supText
        }
        is MathNode.Fraction -> "(${num.toPlainText()})/(${den.toPlainText()})"
        is MathNode.Root -> {
            val prefix = index?.toPlainText()?.let { "[$it]" }.orEmpty()
            "√$prefix(${radicand.toPlainText()})"
        }
        is MathNode.Overline -> base.toPlainText()
    }

private val MATH_SYMBOLS: Map<String, String> =
    mapOf(
        "times" to "×",
        "div" to "÷",
        "cdot" to "·",
        "pm" to "±",
        "mp" to "∓",
        "leq" to "≤",
        "le" to "≤",
        "geq" to "≥",
        "ge" to "≥",
        "neq" to "≠",
        "ne" to "≠",
        "approx" to "≈",
        "equiv" to "≡",
        "sim" to "∼",
        "propto" to "∝",
        "to" to "→",
        "rightarrow" to "→",
        "leftarrow" to "←",
        "gets" to "←",
        "leftrightarrow" to "↔",
        "Rightarrow" to "⇒",
        "Leftarrow" to "⇐",
        "Leftrightarrow" to "⇔",
        "circ" to "∘",
        "infty" to "∞",
        "partial" to "∂",
        "sum" to "∑",
        "prod" to "∏",
        "int" to "∫",
        "angle" to "∠",
        "triangle" to "△",
        "perp" to "⊥",
        "parallel" to "∥",
        "in" to "∈",
        "notin" to "∉",
        "mid" to "∣",
        "Vert" to "‖",
        "subset" to "⊂",
        "subseteq" to "⊆",
        "supset" to "⊃",
        "supseteq" to "⊇",
        "cup" to "∪",
        "cap" to "∩",
        "emptyset" to "∅",
        "forall" to "∀",
        "exists" to "∃",
        "neg" to "¬",
        "land" to "∧",
        "lor" to "∨",
        "prime" to "′",
        "deg" to "°",
        "degree" to "°",
        "therefore" to "∴",
        "because" to "∵",
        "quad" to "  ",
        "qquad" to "    ",
        "alpha" to "α",
        "beta" to "β",
        "gamma" to "γ",
        "delta" to "δ",
        "epsilon" to "ε",
        "varepsilon" to "ε",
        "theta" to "θ",
        "lambda" to "λ",
        "mu" to "μ",
        "pi" to "π",
        "rho" to "ρ",
        "sigma" to "σ",
        "phi" to "φ",
        "omega" to "ω",
        "Delta" to "Δ",
        "Theta" to "Θ",
        "Lambda" to "Λ",
        "Pi" to "Π",
        "Sigma" to "Σ",
        "Phi" to "Φ",
        "Omega" to "Ω",
        "ldots" to "…",
        "dots" to "…",
    )

private val BLACKBOARD_BOLD: Map<Char, String> =
    mapOf(
        'R' to "ℝ",
        'Z' to "ℤ",
        'N' to "ℕ",
        'Q' to "ℚ",
        'C' to "ℂ",
        'H' to "ℍ",
    )

private val FUNCTION_NAMES =
    setOf("sin", "cos", "tan", "cot", "sec", "csc", "log", "ln", "lim", "max", "min", "mod")

private class LatexParser(
    private val input: String,
) {
    private var pos = 0

    fun parseAll(): MathNode = readUntil(null)

    private fun readUntil(stopChar: Char?): MathNode {
        val items = mutableListOf<MathNode>()
        while (pos < input.length) {
            val c = input[pos]
            if (stopChar != null && c == stopChar) {
                pos++
                break
            }
            if (c.isWhitespace()) {
                while (pos < input.length && input[pos].isWhitespace()) pos++
                if (items.isNotEmpty() && (stopChar == null || (pos < input.length && input[pos] != stopChar))) {
                    items += MathNode.Text(" ")
                }
                continue
            }
            items += parseScriptedAtom()
        }
        return simplifyRow(items)
    }

    private fun parseScriptedAtom(): MathNode {
        var node: MathNode? = if (input[pos] == '^' || input[pos] == '_') null else parseAtom()
        var sup: MathNode? = null
        var sub: MathNode? = null
        while (pos < input.length && (input[pos] == '^' || input[pos] == '_')) {
            val op = input[pos++]
            skipSpaces()
            if (op == '^' && isCircNext()) {
                consumeCirc()
                val basePrefix = node
                val degree = MathNode.Text("°")
                node = if (basePrefix == null) degree else MathNode.Row(listOf(basePrefix, degree))
                continue
            }
            val arg = if (pos < input.length) parseAtom() else MathNode.Text("")
            if (op == '^') sup = arg else sub = arg
        }
        if (sup != null || sub != null) {
            return MathNode.Script(node, sup, sub)
        }
        return node ?: MathNode.Text("")
    }

    private fun isCircNext(): Boolean {
        if (input.startsWith("\\circ", pos)) {
            val after = pos + 5
            return after >= input.length || !input[after].isLetter()
        }
        if (input.startsWith("{\\circ}", pos)) return true
        return false
    }

    private fun consumeCirc() {
        pos += if (input.startsWith("{\\circ}", pos)) 7 else 5
    }

    private fun parseAtom(): MathNode {
        skipSpaces()
        if (pos >= input.length) return MathNode.Text("")
        return when (val c = input[pos]) {
            '{' -> {
                pos++
                readUntil('}')
            }
            '\\' -> parseCommand()
            in 'a'..'z', in 'A'..'Z' -> {
                pos++
                MathNode.Text(c.toString(), italic = true)
            }
            in '0'..'9' -> {
                val start = pos
                while (pos < input.length && (input[pos].isDigit() || ((input[pos] == '.' || input[pos] == ',') && pos + 1 < input.length && input[pos + 1].isDigit()))) {
                    pos++
                }
                MathNode.Text(input.substring(start, pos))
            }
            else -> {
                pos++
                MathNode.Text(c.toString())
            }
        }
    }

    private fun parseCommand(): MathNode {
        pos++
        if (pos >= input.length) return MathNode.Text("")
        val first = input[pos]
        if (!first.isLetter()) {
            pos++
            return when (first) {
                ',', ';', ':', ' ' -> MathNode.Text(" ")
                '!' -> MathNode.Text("")
                '|' -> MathNode.Text("‖")
                '\\', '%', '{', '}', '&', '$', '_', '#' -> MathNode.Text(first.toString())
                else -> MathNode.Text(first.toString())
            }
        }
        val start = pos
        while (pos < input.length && input[pos].isLetter()) pos++
        val cmd = input.substring(start, pos)
        return when (cmd) {
            "frac", "dfrac", "tfrac" -> {
                val num = parseAtom()
                val den = parseAtom()
                MathNode.Fraction(num, den)
            }
            "sqrt" -> {
                skipSpaces()
                val index =
                    if (pos < input.length && input[pos] == '[') {
                        pos++
                        readUntil(']')
                    } else {
                        null
                    }
                val radicand = parseAtom()
                MathNode.Root(radicand, index)
            }
            "text", "mathrm", "mathbf", "textbf", "operatorname" -> MathNode.Text(readBracedLiteral(), italic = false)
            "textit" -> MathNode.Text(readBracedLiteral(), italic = true)
            "mathbb" -> {
                val raw = readBracedLiteral().trim()
                val mapped = raw.map { ch -> BLACKBOARD_BOLD[ch] ?: ch.toString() }.joinToString("")
                MathNode.Text(mapped)
            }
            "overline", "bar" -> MathNode.Overline(parseAtom())
            "left", "right", "displaystyle", "textstyle" -> {
                skipSpaces()
                if (cmd == "left" || cmd == "right") {
                    if (pos < input.length && input[pos] == '.') {
                        pos++
                        MathNode.Text("")
                    } else if (pos < input.length) {
                        parseAtom()
                    } else {
                        MathNode.Text("")
                    }
                } else {
                    MathNode.Text("")
                }
            }
            in FUNCTION_NAMES -> MathNode.Text(cmd)
            else -> {
                val symbol = MATH_SYMBOLS[cmd]
                MathNode.Text(symbol ?: cmd)
            }
        }
    }

    private fun readBracedLiteral(): String {
        skipSpaces()
        if (pos >= input.length) return ""
        if (input[pos] != '{') {
            val ch = input[pos++]
            return ch.toString()
        }
        pos++
        val start = pos
        var depth = 1
        while (pos < input.length && depth > 0) {
            when (input[pos]) {
                '{' -> depth++
                '}' -> depth--
            }
            if (depth > 0) pos++
        }
        val content = input.substring(start, pos)
        if (pos < input.length && input[pos] == '}') pos++
        return content
    }

    private fun skipSpaces() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }
}

private fun simplifyRow(items: List<MathNode>): MathNode {
    val flat =
        items.filterNot { it is MathNode.Text && it.value.isEmpty() }
    return when (flat.size) {
        0 -> MathNode.Text("")
        1 -> flat[0]
        else -> MathNode.Row(flat)
    }
}

data class RenderedSpanContent(
    val text: AnnotatedString,
    val inlineContent: Map<String, InlineTextContent>,
)

fun MathNode.hasBlockLayout(): Boolean =
    when (this) {
        is MathNode.Fraction, is MathNode.Root, is MathNode.Overline -> true
        is MathNode.Row -> items.any { it.hasBlockLayout() }
        is MathNode.Script ->
            (base?.hasBlockLayout() == true) ||
                (sup?.hasBlockLayout() == true) ||
                (sub?.hasBlockLayout() == true)
        is MathNode.Text -> false
    }

fun hasBlockMath(spans: List<HtmlSpan>): Boolean = spans.any { it is HtmlSpan.Math && parseLatex(it.latex).hasBlockLayout() }

sealed interface FlowPiece {
    data class StyledText(
        val text: AnnotatedString,
    ) : FlowPiece

    data class BlockMath(
        val node: MathNode,
    ) : FlowPiece
}

fun tokenizeFlowLines(
    spans: List<HtmlSpan>,
    fontSize: Int,
): List<List<List<FlowPiece>>> {
    val lines = mutableListOf<MutableList<List<FlowPiece>>>()
    var currentLine = mutableListOf<List<FlowPiece>>()
    var currentCluster = mutableListOf<FlowPiece>()

    fun flushCluster() {
        if (currentCluster.isNotEmpty()) {
            currentLine += currentCluster.toList()
            currentCluster = mutableListOf()
        }
    }

    fun flushLine() {
        flushCluster()
        lines += currentLine
        currentLine = mutableListOf()
    }

    fun appendAnnotatedWord(word: AnnotatedString) {
        if (word.isEmpty()) return
        val last = currentCluster.lastOrNull()
        if (last is FlowPiece.StyledText) {
            currentCluster[currentCluster.lastIndex] =
                FlowPiece.StyledText(
                    buildAnnotatedString {
                        append(last.text)
                        append(word)
                    },
                )
        } else {
            currentCluster += FlowPiece.StyledText(word)
        }
    }

    fun pushStyledString(
        raw: String,
        style: SpanStyle,
    ) {
        var idx = 0
        while (idx < raw.length) {
            if (raw[idx].isWhitespace()) {
                while (idx < raw.length && raw[idx].isWhitespace()) idx++
                flushCluster()
            } else {
                val start = idx
                while (idx < raw.length && !raw[idx].isWhitespace()) idx++
                val token = raw.substring(start, idx)
                appendAnnotatedWord(buildAnnotatedString { withStyle(style) { append(token) } })
            }
        }
    }

    fun pushMathNode(node: MathNode) {
        if (!node.hasBlockLayout()) {
            when (node) {
                is MathNode.Text -> pushStyledString(node.value, SpanStyle(fontStyle = if (node.italic) FontStyle.Italic else FontStyle.Normal))
                is MathNode.Row -> node.items.forEach(::pushMathNode)
                else -> appendAnnotatedWord(buildMathInlineAnnotated(node, fontSize.toFloat()))
            }
            return
        }
        when (node) {
            is MathNode.Row -> node.items.forEach(::pushMathNode)
            else -> currentCluster += FlowPiece.BlockMath(node)
        }
    }

    for (span in spans) {
        when (span) {
            HtmlSpan.LineBreak -> flushLine()
            is HtmlSpan.Text -> pushStyledString(span.text, span.toSpanStyle())
            is HtmlSpan.Math -> pushMathNode(parseLatex(span.latex))
        }
    }
    flushCluster()
    if (currentLine.isNotEmpty() || lines.isEmpty()) {
        lines += currentLine
    }
    return lines
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MathFlowParagraph(
    spans: List<HtmlSpan>,
    fontSize: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val lines = androidx.compose.runtime.remember(spans, fontSize) { tokenizeFlowLines(spans, fontSize) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (clusters in lines) {
            if (clusters.isEmpty()) continue
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (cluster in clusters) {
                    if (cluster.size == 1 && cluster[0] is FlowPiece.StyledText) {
                        Text(
                            text = (cluster[0] as FlowPiece.StyledText).text,
                            style = mathTextStyle(fontSize.toFloat(), color),
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        ) {
                            for (piece in cluster) {
                                when (piece) {
                                    is FlowPiece.StyledText ->
                                        Text(
                                            text = piece.text,
                                            style = mathTextStyle(fontSize.toFloat(), color),
                                        )
                                    is FlowPiece.BlockMath ->
                                        MathBlockView(
                                            node = piece.node,
                                            fontSize = fontSize.toFloat(),
                                            color = color,
                                        )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun buildRichSpans(
    spans: List<HtmlSpan>,
    fontSize: Int,
    color: Color,
    textMeasurer: TextMeasurer,
    density: Density,
): RenderedSpanContent {
    val inlineContent = mutableMapOf<String, InlineTextContent>()
    var keyIndex = 0
    val annotated =
        buildAnnotatedString {
            for (span in spans) {
                when (span) {
                    HtmlSpan.LineBreak -> append('\n')
                    is HtmlSpan.Text ->
                        withStyle(span.toSpanStyle()) { append(span.text) }
                    is HtmlSpan.Math -> {
                        val node = parseLatex(span.latex)
                        appendMathNode(
                            node = node,
                            fontSize = fontSize.toFloat(),
                            color = color,
                            textMeasurer = textMeasurer,
                            density = density,
                            nextKey = { "math_${keyIndex++}" },
                            inlineContent = inlineContent,
                        )
                    }
                }
            }
        }
    return RenderedSpanContent(annotated, inlineContent)
}

private fun AnnotatedString.Builder.appendMathNode(
    node: MathNode,
    fontSize: Float,
    color: Color,
    textMeasurer: TextMeasurer,
    density: Density,
    nextKey: () -> String,
    inlineContent: MutableMap<String, InlineTextContent>,
) {
    when (node) {
        is MathNode.Row ->
            for (item in node.items) {
                appendMathNode(item, fontSize, color, textMeasurer, density, nextKey, inlineContent)
            }
        is MathNode.Text ->
            withStyle(
                SpanStyle(
                    fontStyle = if (node.italic) FontStyle.Italic else FontStyle.Normal,
                    fontSize = fontSize.sp,
                ),
            ) { append(node.value) }
        is MathNode.Script -> {
            if (node.base != null) {
                appendMathNode(node.base, fontSize, color, textMeasurer, density, nextKey, inlineContent)
            }
            val scriptSize = max(9f, fontSize * SCRIPT_SCALE)
            if (node.sub != null) {
                appendScriptPart(
                    part = node.sub,
                    shift = BaselineShift.Subscript,
                    scriptSize = scriptSize,
                    color = color,
                    textMeasurer = textMeasurer,
                    density = density,
                    nextKey = nextKey,
                    inlineContent = inlineContent,
                )
            }
            if (node.sup != null) {
                appendScriptPart(
                    part = node.sup,
                    shift = BaselineShift.Superscript,
                    scriptSize = scriptSize,
                    color = color,
                    textMeasurer = textMeasurer,
                    density = density,
                    nextKey = nextKey,
                    inlineContent = inlineContent,
                )
            }
        }
        is MathNode.Fraction, is MathNode.Root, is MathNode.Overline -> {
            val sizePx = measureMathPx(node, fontSize, textMeasurer, density)
            val key = nextKey()
            val widthSp = with(density) { sizePx.width.toSp() }
            val heightSp = with(density) { sizePx.height.toSp() }
            inlineContent[key] =
                InlineTextContent(
                    Placeholder(
                        width = widthSp,
                        height = heightSp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                    ),
                ) {
                    MathBlockView(node = node, fontSize = fontSize, color = color)
                }
            appendInlineContent(key, node.toPlainText().ifEmpty { " " })
        }
    }
}

private fun AnnotatedString.Builder.appendScriptPart(
    part: MathNode,
    shift: BaselineShift,
    scriptSize: Float,
    color: Color,
    textMeasurer: TextMeasurer,
    density: Density,
    nextKey: () -> String,
    inlineContent: MutableMap<String, InlineTextContent>,
) {
    if (!part.hasBlockLayout()) {
        withStyle(SpanStyle(baselineShift = shift, fontSize = scriptSize.sp)) {
            appendMathInlineText(part, scriptSize)
        }
    } else {
        appendMathNode(part, scriptSize, color, textMeasurer, density, nextKey, inlineContent)
    }
}

private fun AnnotatedString.Builder.appendMathInlineText(
    node: MathNode,
    fontSize: Float,
) {
    when (node) {
        is MathNode.Row -> node.items.forEach { appendMathInlineText(it, fontSize) }
        is MathNode.Text ->
            withStyle(SpanStyle(fontStyle = if (node.italic) FontStyle.Italic else FontStyle.Normal)) {
                append(node.value)
            }
        is MathNode.Script -> {
            node.base?.let { appendMathInlineText(it, fontSize) }
            val nested = max(8f, fontSize * SCRIPT_SCALE)
            node.sub?.let {
                withStyle(SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = nested.sp)) {
                    appendMathInlineText(it, nested)
                }
            }
            node.sup?.let {
                withStyle(SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = nested.sp)) {
                    appendMathInlineText(it, nested)
                }
            }
        }
        else -> append(node.toPlainText())
    }
}

private data class PxSize(
    val width: Float,
    val height: Float,
)

private fun mathTextStyle(
    fontSize: Float,
    color: Color = Color.Unspecified,
    fontStyle: FontStyle = FontStyle.Normal,
): TextStyle =
    TextStyle(
        fontFamily = DMSansFontFamily,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.28f).sp,
        fontStyle = fontStyle,
        color = color,
    )

private fun buildMathInlineAnnotated(
    node: MathNode,
    fontSize: Float,
): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(fontSize = fontSize.sp)) {
            appendMathInlineText(node, fontSize)
        }
    }

private fun measureMathPx(
    node: MathNode,
    fontSize: Float,
    textMeasurer: TextMeasurer,
    density: Density,
): PxSize {
    if (!node.hasBlockLayout()) {
        val annotated = buildMathInlineAnnotated(node, fontSize)
        val result =
            textMeasurer.measure(
                text = annotated,
                style = mathTextStyle(fontSize),
                softWrap = false,
                maxLines = 1,
            )
        val extraTopPx = if (node.hasScript()) with(density) { 4.dp.toPx() } else 0f
        return PxSize(
            width = ceil(result.size.width.toFloat()) + 2f,
            height = ceil(result.size.height.toFloat() + extraTopPx) + 2f,
        )
    }
    return when (node) {
        is MathNode.Text -> PxSize(0f, 0f)
        is MathNode.Script -> {
            val baseSize = node.base?.let { measureMathPx(it, fontSize, textMeasurer, density) } ?: PxSize(0f, 0f)
            val scriptSize = max(9f, fontSize * SCRIPT_SCALE)
            val supSize = node.sup?.let { measureMathPx(it, scriptSize, textMeasurer, density) } ?: PxSize(0f, 0f)
            val subSize = node.sub?.let { measureMathPx(it, scriptSize, textMeasurer, density) } ?: PxSize(0f, 0f)
            val extraW = max(supSize.width, subSize.width)
            val h = max(baseSize.height, supSize.height + subSize.height)
            PxSize(baseSize.width + extraW, h)
        }
        is MathNode.Row -> {
            val grouped = groupInlineRuns(node.items)
            var width = 0f
            var height = 0f
            for (item in grouped) {
                val s = measureMathPx(item, fontSize, textMeasurer, density)
                width += s.width
                height = max(height, s.height)
            }
            PxSize(width, height)
        }
        is MathNode.Fraction -> {
            val childFont = max(10f, fontSize * FRACTION_SCALE)
            val numSize = measureMathPx(node.num, childFont, textMeasurer, density)
            val denSize = measureMathPx(node.den, childFont, textMeasurer, density)
            val padPx = with(density) { 10.dp.toPx() }
            val barPx = with(density) { 7.dp.toPx() }
            PxSize(
                width = ceil(max(numSize.width, denSize.width) + padPx),
                height = ceil(numSize.height + denSize.height + barPx),
            )
        }
        is MathNode.Root -> {
            val childSize = measureMathPx(node.radicand, fontSize, textMeasurer, density)
            val indexSize =
                node.index?.let { measureMathPx(it, max(8f, fontSize * 0.58f), textMeasurer, density) }
            val hookPx = with(density) { 12.dp.toPx() }
            val hookAreaPx =
                if (indexSize == null) {
                    hookPx + with(density) { 2.dp.toPx() }
                } else {
                    max(hookPx + with(density) { 2.dp.toPx() }, indexSize.width + hookPx * 0.65f)
                }
            val innerPadPx = with(density) { 11.dp.toPx() }
            val topBottomPx = with(density) { 9.dp.toPx() }
            val minIndexHeight = if (indexSize == null) 0f else indexSize.height * 1.4f + with(density) { 6.dp.toPx() }
            PxSize(
                width = ceil(hookAreaPx + childSize.width + innerPadPx),
                height = ceil(max(childSize.height + topBottomPx, minIndexHeight)),
            )
        }
        is MathNode.Overline -> {
            val childSize = measureMathPx(node.base, fontSize, textMeasurer, density)
            val barPx = with(density) { 6.dp.toPx() }
            PxSize(ceil(childSize.width + with(density) { 6.dp.toPx() }), ceil(childSize.height + barPx))
        }
    }
}

private fun MathNode.hasScript(): Boolean =
    when (this) {
        is MathNode.Script -> true
        is MathNode.Row -> items.any { it.hasScript() }
        is MathNode.Fraction -> num.hasScript() || den.hasScript()
        is MathNode.Root -> radicand.hasScript() || (index?.hasScript() == true)
        is MathNode.Overline -> base.hasScript()
        is MathNode.Text -> false
    }

private fun groupInlineRuns(items: List<MathNode>): List<MathNode> {
    if (items.size <= 1) return items
    val out = mutableListOf<MathNode>()
    val inlineBuf = mutableListOf<MathNode>()

    fun flush() {
        if (inlineBuf.isNotEmpty()) {
            out += if (inlineBuf.size == 1) inlineBuf[0] else MathNode.Row(inlineBuf.toList())
            inlineBuf.clear()
        }
    }
    for (item in items) {
        if (!item.hasBlockLayout()) {
            inlineBuf += item
        } else {
            flush()
            out += item
        }
    }
    flush()
    return out
}

@Composable
private fun MathBlockView(
    node: MathNode,
    fontSize: Float,
    color: Color,
) {
    when (node) {
        is MathNode.Fraction -> {
            val childFont = max(10f, fontSize * FRACTION_SCALE)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(IntrinsicSize.Max).padding(horizontal = 4.dp),
            ) {
                MathInlineView(node = node.num, fontSize = childFont, color = color)
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .height(1.5.dp),
                ) {
                    drawLine(
                        color = color,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 1.2.dp.toPx(),
                    )
                }
                MathInlineView(node = node.den, fontSize = childFont, color = color)
            }
        }
        is MathNode.Root -> {
            val hookWidth: Dp = 12.dp
            val density = LocalDensity.current
            val textMeasurer = rememberTextMeasurer()
            val indexStartPad: Dp =
                if (node.index != null) {
                    val indexW = measureMathPx(node.index, max(8f, fontSize * 0.58f), textMeasurer, density).width
                    maxOf(hookWidth + 2.dp, with(density) { indexW.toDp() } + hookWidth * 0.65f)
                } else {
                    hookWidth + 2.dp
                }
            Box(modifier = Modifier.padding(top = 3.dp, end = 2.dp, bottom = 2.dp)) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val stroke = 1.25.dp.toPx()
                    val hookPx = hookWidth.toPx()
                    val startPadPx = indexStartPad.toPx()
                    val hookLeft = (startPadPx - hookPx).coerceAtLeast(0f)
                    val topY = stroke / 2f
                    val path =
                        Path().apply {
                            moveTo(hookLeft, size.height * 0.58f)
                            lineTo(hookLeft + hookPx * 0.30f, size.height * 0.50f)
                            lineTo(hookLeft + hookPx * 0.60f, size.height - stroke)
                            lineTo(startPadPx, topY)
                            lineTo(size.width, topY)
                        }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                if (node.index != null) {
                    Box(modifier = Modifier.align(Alignment.TopStart).padding(start = 1.dp)) {
                        MathInlineView(node = node.index, fontSize = max(8f, fontSize * 0.58f), color = color)
                    }
                }
                Box(modifier = Modifier.padding(start = indexStartPad + 3.dp, top = 3.dp, end = 3.dp, bottom = 1.dp)) {
                    MathInlineView(node = node.radicand, fontSize = fontSize, color = color)
                }
            }
        }
        is MathNode.Overline -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(IntrinsicSize.Max).padding(horizontal = 2.dp),
            ) {
                Canvas(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp)
                            .height(1.dp),
                ) {
                    drawLine(
                        color = color,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 1.1.dp.toPx(),
                    )
                }
                MathInlineView(node = node.base, fontSize = fontSize, color = color)
            }
        }
        else -> MathInlineView(node = node, fontSize = fontSize, color = color)
    }
}

@Composable
private fun MathInlineView(
    node: MathNode,
    fontSize: Float,
    color: Color,
) {
    if (!node.hasBlockLayout()) {
        val text = buildMathInlineAnnotated(node, fontSize)
        Text(
            text = text,
            style = mathTextStyle(fontSize, color),
            softWrap = false,
            maxLines = 1,
        )
    } else {
        when (node) {
            is MathNode.Row ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (item in groupInlineRuns(node.items)) {
                        MathInlineView(node = item, fontSize = fontSize, color = color)
                    }
                }
            is MathNode.Script -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    node.base?.let { MathInlineView(node = it, fontSize = fontSize, color = color) }
                    val scriptFont = max(9f, fontSize * SCRIPT_SCALE)
                    Column {
                        node.sup?.let { MathInlineView(node = it, fontSize = scriptFont, color = color) }
                        node.sub?.let { MathInlineView(node = it, fontSize = scriptFont, color = color) }
                    }
                }
            }
            else -> MathBlockView(node = node, fontSize = fontSize, color = color)
        }
    }
}

private const val SCRIPT_SCALE = 0.72f
private const val FRACTION_SCALE = 0.80f
