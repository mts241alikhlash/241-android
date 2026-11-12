package id.sch.mts241alikhlash.exam.ui.exam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import id.sch.mts241alikhlash.exam.api.exam.models.PaperChoiceDto
import id.sch.mts241alikhlash.exam.api.exam.models.PaperQuestionDto
import id.sch.mts241alikhlash.exam.data.model.AnswerPayload
import id.sch.mts241alikhlash.exam.ui.components.FloatingTextField
import id.sch.mts241alikhlash.exam.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.exam.ui.theme.TextMuted
import id.sch.mts241alikhlash.exam.ui.theme.WebTokens

data class ImageContext(
    val sessionId: String,
    val baseUrl: String,
    val imageLoader: ImageLoader?,
)

fun AnswerPayload?.isFilled(): Boolean =
    when (this) {
        null -> false
        is AnswerPayload.Multiple -> keys.isNotEmpty()
        is AnswerPayload.Text -> text.isNotBlank()
        else -> true
    }

@Composable
fun QuestionAnswerInput(
    question: PaperQuestionDto,
    answer: AnswerPayload?,
    draft: String?,
    enabled: Boolean,
    images: ImageContext,
    onAnswer: (AnswerPayload) -> Unit,
    onToggleKey: (String) -> Unit,
    onTextChange: (String) -> Unit,
) {
    val choices = question.choices.orEmpty()
    when (question.type.value) {
        "MULTIPLE_CHOICE" ->
            ChoiceInput(
                choices = choices,
                selected = (answer as? AnswerPayload.Choice)?.key,
                enabled = enabled,
                images = images,
                onSelect = { onAnswer(AnswerPayload.Choice(it)) },
            )
        "MULTIPLE_RESPONSE" ->
            MultipleResponseInput(
                choices = choices,
                selected = (answer as? AnswerPayload.Multiple)?.keys.orEmpty().toSet(),
                enabled = enabled,
                images = images,
                onToggle = onToggleKey,
            )
        "TRUE_FALSE" ->
            TrueFalseInput(
                selected = (answer as? AnswerPayload.Bool)?.value,
                enabled = enabled,
                onSelect = { onAnswer(AnswerPayload.Bool(it)) },
            )
        "SHORT_ANSWER" ->
            TextAnswerInput(
                value = draft ?: (answer as? AnswerPayload.Text)?.text.orEmpty(),
                multiline = false,
                enabled = enabled,
                onChange = onTextChange,
            )
        else ->
            TextAnswerInput(
                value = draft ?: (answer as? AnswerPayload.Text)?.text.orEmpty(),
                multiline = true,
                enabled = enabled,
                onChange = onTextChange,
            )
    }
}

@Composable
fun ChoiceInput(
    choices: List<PaperChoiceDto>,
    selected: String?,
    enabled: Boolean,
    images: ImageContext,
    onSelect: (String) -> Unit,
) {
    Column {
        choices.forEachIndexed { index, choice ->
            ChoiceRow(
                letter = ('A' + index).toString(),
                html = choice.html,
                selected = choice.key.value == selected,
                multi = false,
                enabled = enabled,
                images = images,
                onClick = { onSelect(choice.key.value) },
            )
        }
    }
}

@Composable
fun MultipleResponseInput(
    choices: List<PaperChoiceDto>,
    selected: Set<String>,
    enabled: Boolean,
    images: ImageContext,
    onToggle: (String) -> Unit,
) {
    Column {
        Text(
            text = "Pilih semua jawaban yang benar",
            fontFamily = DMSansFontFamily,
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        choices.forEachIndexed { index, choice ->
            ChoiceRow(
                letter = ('A' + index).toString(),
                html = choice.html,
                selected = choice.key.value in selected,
                multi = true,
                enabled = enabled,
                images = images,
                onClick = { onToggle(choice.key.value) },
            )
        }
    }
}

@Composable
fun TrueFalseInput(
    selected: Boolean?,
    enabled: Boolean,
    onSelect: (Boolean) -> Unit,
) {
    Column {
        ChoiceRow("B", "Benar", selected == true, multi = false, enabled = enabled, images = null, onClick = { onSelect(true) })
        ChoiceRow("S", "Salah", selected == false, multi = false, enabled = enabled, images = null, onClick = { onSelect(false) })
    }
}

@Composable
fun TextAnswerInput(
    value: String,
    multiline: Boolean,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    Column {
        FloatingTextField(
            value = value,
            onValueChange = onChange,
            label = if (multiline) "Jawaban Anda" else "Jawaban singkat",
            enabled = enabled,
            singleLine = !multiline,
            minHeight = if (multiline) 160.dp else WebTokens.ControlHeight,
            labelBackground = WebTokens.Card,
            modifier = Modifier.fillMaxWidth(),
        )
        if (value.length >= COUNTER_FROM) {
            Text(
                text = "${value.length} / $LIMIT",
                fontFamily = DMSansFontFamily,
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private const val COUNTER_FROM = 15_000
private const val LIMIT = 20_000

@Composable
private fun ChoiceRow(
    letter: String,
    html: String,
    selected: Boolean,
    multi: Boolean,
    enabled: Boolean,
    images: ImageContext?,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(WebTokens.Radius)
    val borderColor = if (selected) WebTokens.Primary else WebTokens.InputBorder
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .heightIn(min = WebTokens.ControlHeight)
                .height(IntrinsicSize.Min)
                .clip(shape)
                .background(if (selected) WebTokens.Primary.copy(alpha = 0.06f) else WebTokens.Card)
                .border(1.dp, borderColor, shape)
                .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .width(WebTokens.ControlHeight)
                    .fillMaxHeight()
                    .background(if (selected) WebTokens.Primary else WebTokens.Foreground.copy(alpha = 0.04f)),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Text(
                text = letter,
                fontFamily = DMSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (selected) WebTokens.PrimaryForeground else WebTokens.MutedForeground,
            )
        }
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(borderColor),
        )

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            if (images != null) {
                HtmlContent(
                    html = html,
                    sessionId = images.sessionId,
                    baseUrl = images.baseUrl,
                    imageLoader = images.imageLoader,
                    fontSize = 14,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = html,
                    fontFamily = DMSansFontFamily,
                    fontSize = 14.sp,
                    color = WebTokens.Foreground,
                )
            }
        }

        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = WebTokens.Primary,
                modifier =
                    Modifier
                        .padding(end = 10.dp)
                        .size(14.dp),
            )
        }
    }
}
