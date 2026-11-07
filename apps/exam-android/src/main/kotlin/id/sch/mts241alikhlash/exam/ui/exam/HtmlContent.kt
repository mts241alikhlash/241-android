package id.sch.mts241alikhlash.exam.ui.exam

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import id.sch.mts241alikhlash.exam.ui.theme.BorderSubtle
import id.sch.mts241alikhlash.exam.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.exam.ui.theme.TextMuted
import id.sch.mts241alikhlash.exam.ui.theme.TextPrimary

fun examFileUrl(
    baseUrl: String,
    sessionId: String,
    fileId: String,
): String = "${baseUrl.trimEnd('/')}/student-exams/$sessionId/files/$fileId"

fun List<HtmlSpan>.toAnnotated(): AnnotatedString =
    buildAnnotatedString {
        for (span in this@toAnnotated) {
            when (span) {
                HtmlSpan.LineBreak -> append('\n')
                is HtmlSpan.Text ->
                    withStyle(
                        SpanStyle(
                            fontWeight = if (span.bold) FontWeight.Bold else null,
                            fontStyle = if (span.italic) FontStyle.Italic else null,
                            textDecoration = if (span.underline) TextDecoration.Underline else null,
                            baselineShift =
                                when {
                                    span.subscript -> BaselineShift.Subscript
                                    span.superscript -> BaselineShift.Superscript
                                    else -> null
                                },
                            fontSize = if (span.subscript || span.superscript) 11.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                        ),
                    ) { append(span.text) }
            }
        }
    }

@Composable
fun HtmlContent(
    html: String,
    sessionId: String,
    baseUrl: String,
    imageLoader: ImageLoader?,
    modifier: Modifier = Modifier,
    fontSize: Int = 15,
) {
    val blocks = remember(html) { parseHtml(html) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (block in blocks) {
            when (block) {
                is HtmlBlock.Paragraph -> DirectedText(block.spans, block.rtl, fontSize)
                is HtmlBlock.ListBlock ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        block.items.forEachIndexed { index, item ->
                            Row {
                                Text(
                                    text = if (block.ordered) "${index + 1}. " else "• ",
                                    fontFamily = DMSansFontFamily,
                                    fontSize = fontSize.sp,
                                    color = TextPrimary,
                                )
                                DirectedText(item, rtl = false, fontSize = fontSize)
                            }
                        }
                    }
                is HtmlBlock.Table ->
                    Column(modifier = Modifier.border(1.dp, BorderSubtle)) {
                        for (row in block.rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (cell in row) {
                                    Box(modifier = Modifier.weight(1f).border(0.5.dp, BorderSubtle).padding(6.dp)) {
                                        DirectedText(cell, rtl = false, fontSize = fontSize - 1)
                                    }
                                }
                            }
                        }
                    }
                is HtmlBlock.Image ->
                    ExamImage(
                        url = examFileUrl(baseUrl, sessionId, block.fileId),
                        imageLoader = imageLoader,
                    )
            }
        }
    }
}

@Composable
private fun DirectedText(
    spans: List<HtmlSpan>,
    rtl: Boolean,
    fontSize: Int,
) {
    val direction = if (rtl) LayoutDirection.Rtl else LocalLayoutDirection.current
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        Text(
            text = remember(spans) { spans.toAnnotated() },
            fontFamily = DMSansFontFamily,
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 7).sp,
            color = TextPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExamImage(
    url: String,
    imageLoader: ImageLoader?,
) {
    val context = LocalContext.current
    var attempt by remember { mutableIntStateOf(0) }
    val loader = imageLoader ?: return
    val request =
        remember(url, attempt) {
            ImageRequest
                .Builder(context)
                .data(url)
                .memoryCacheKey("$url#$attempt")
                .build()
        }
    SubcomposeAsyncImage(
        model = request,
        imageLoader = loader,
        contentDescription = null,
        modifier = Modifier.fillMaxWidth(),
        error = {
            Column(
                modifier = Modifier.fillMaxWidth().border(1.dp, BorderSubtle).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Gambar tidak dapat dimuat",
                    fontFamily = DMSansFontFamily,
                    fontSize = 12.sp,
                    color = TextMuted,
                )
                TextButton(onClick = { attempt++ }) { Text("Muat ulang") }
            }
        },
    )
}
