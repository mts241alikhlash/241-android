package id.sch.mts241alikhlash.cbt.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import id.sch.mts241alikhlash.cbt.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.cbt.ui.theme.WebTokens

enum class ActionStyle { Primary, Outline, Destructive }

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ActionStyle = ActionStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false
) {
    val shape = RoundedCornerShape(WebTokens.Radius)
    val container = when (style) {
        ActionStyle.Primary -> WebTokens.Primary
        ActionStyle.Destructive -> WebTokens.Destructive
        ActionStyle.Outline -> Color.Transparent
    }
    val content = if (style == ActionStyle.Outline) WebTokens.Foreground else WebTokens.PrimaryForeground
    Box(
        modifier = modifier
            .height(WebTokens.ControlHeight)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .background(container)
            .then(if (style == ActionStyle.Outline) Modifier.border(1.dp, WebTokens.Border, shape) else Modifier)
            .clickable(enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = content
            )
        } else {
            Text(
                text = text,
                fontSize = WebTokens.ControlTextSize,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSansFontFamily,
                color = content,
                maxLines = 1
            )
        }
    }
}

/**
 * The web's `DialogContent size="md"` with a standard header and footer: 8dp corners, a muted
 * header and footer separated by hairlines, a 16dp body and, when a footer has two actions,
 * the secondary on the left and the primary on the right.
 */
@Composable
fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    dismissible: Boolean = true,
    footerStart: (@Composable () -> Unit)? = null,
    footerEnd: (@Composable () -> Unit)? = null,
    footerContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false
        )
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0.8f) }

        val shape = RoundedCornerShape(WebTokens.Radius)
        val maxHeight = LocalConfiguration.current.screenHeightDp.dp - 32.dp
        Column(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 448.dp)
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .clip(shape)
                .background(WebTokens.Background)
                .border(1.dp, WebTokens.Border, shape)
        ) {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .background(WebTokens.Foreground.copy(alpha = 0.03f))
                        .padding(start = 16.dp, end = 48.dp, top = 2.dp, bottom = 2.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    HeaderTitle(title)
                    if (description != null) {
                        Text(
                            text = description,
                            fontSize = WebTokens.ControlTextSize,
                            fontFamily = DMSansFontFamily,
                            color = WebTokens.MutedForeground
                        )
                    }
                }
                if (dismissible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 8.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(WebTokens.Radius))
                            .clickable(role = Role.Button, onClick = onDismiss)
                            .semantics { contentDescription = "Tutup" },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            val stroke = 1.5.dp.toPx()
                            drawLine(
                                WebTokens.Foreground, Offset(0f, 0f), Offset(size.width, size.height),
                                stroke, StrokeCap.Round
                            )
                            drawLine(
                                WebTokens.Foreground, Offset(size.width, 0f), Offset(0f, size.height),
                                stroke, StrokeCap.Round
                            )
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(WebTokens.Border))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                content = content
            )
            if (footerStart != null || footerEnd != null || footerContent != null) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(WebTokens.Border))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .background(WebTokens.Foreground.copy(alpha = 0.03f))
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = if (footerStart != null && footerEnd != null) {
                        Arrangement.SpaceBetween
                    } else if (footerEnd != null) {
                        Arrangement.End
                    } else {
                        Arrangement.Start
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    footerContent?.invoke()
                    footerStart?.invoke()
                    footerEnd?.invoke()
                }
            }
        }
    }
}
