package id.sch.mts241alikhlash.istiqra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.istiqra.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.istiqra.ui.theme.WebTokens

@Composable
fun FloatingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    password: Boolean = false,
    singleLine: Boolean = true,
    minHeight: Dp = WebTokens.ControlHeight,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    labelBackground: Color = WebTokens.Card,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focused by interactionSource.collectIsFocusedAsState()
    val floated = labelFloated(focused, value, error)
    val progress by animateFloatAsState(
        targetValue = if (floated) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "floating-label",
    )
    val labelSize = (WebTokens.ControlTextSize.value + (WebTokens.LabelFloatedSize.value - WebTokens.ControlTextSize.value) * progress).sp
    val restColor = WebTokens.MutedForeground
    val activeColor = labelColor(focused, error)
    val color = if (floated) activeColor else restColor
    val shape = RoundedCornerShape(WebTokens.Radius)

    Box(
        modifier =
            modifier
                .padding(top = WebTokens.LabelZone)
                .alpha(if (enabled) 1f else 0.5f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled,
                ) {
                    focusRequester.requestFocus()
                    keyboard?.show()
                }.semantics(mergeDescendants = true) {},
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .border(1.dp, borderColor(focused, error), shape),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                interactionSource = interactionSource,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                cursorBrush = SolidColor(WebTokens.Primary),
                textStyle =
                    TextStyle(
                        fontFamily = DMSansFontFamily,
                        fontSize = WebTokens.FieldTextSize,
                        color = WebTokens.Foreground,
                    ),
                modifier =
                    Modifier
                        .focusRequester(focusRequester)
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            top = if (singleLine) WebTokens.FilledTextShift * 2 * progress else 10.dp + WebTokens.FilledTextShift * 2 * progress,
                            bottom = if (singleLine) 0.dp else 10.dp,
                        ).fillMaxWidthField(),
            )
        }
        Text(
            text = labelText(label, error),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = color,
            fontSize = labelSize,
            fontFamily = DMSansFontFamily,
            fontWeight = FontWeight.Normal,
            modifier =
                Modifier
                    .layout { measurable, constraints ->
                        val maxWidth = (constraints.maxWidth - (WebTokens.LabelInset * 2).roundToPx()).coerceAtLeast(0)
                        val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = maxWidth))
                        val restCenter = WebTokens.ControlHeight.roundToPx() / 2f
                        val centerY = restCenter * (1f - progress)
                        layout(placeable.width, 0) {
                            placeable.place(WebTokens.LabelInset.roundToPx(), (centerY - placeable.height / 2f).toInt())
                        }
                    }.background(labelBackground)
                    .padding(horizontal = 2.dp),
        )
    }
}

private fun Modifier.fillMaxWidthField(): Modifier =
    this.then(
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = constraints.maxWidth))
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        },
    )
