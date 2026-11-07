package id.sch.mts241alikhlash.exam.ui.components

import androidx.compose.ui.graphics.Color
import id.sch.mts241alikhlash.exam.ui.theme.WebTokens

internal fun labelFloated(
    focused: Boolean,
    value: String,
    error: String?,
): Boolean = focused || value.isNotEmpty() || error != null

internal fun labelText(
    label: String,
    error: String?,
): String = error ?: label

internal fun labelColor(
    focused: Boolean,
    error: String?,
): Color =
    when {
        error != null -> WebTokens.Destructive
        focused -> WebTokens.Primary
        else -> WebTokens.Foreground.copy(alpha = 0.7f)
    }

internal fun borderColor(
    focused: Boolean,
    error: String?,
): Color =
    when {
        error != null -> WebTokens.Destructive
        focused -> WebTokens.Primary
        else -> WebTokens.InputBorder
    }
