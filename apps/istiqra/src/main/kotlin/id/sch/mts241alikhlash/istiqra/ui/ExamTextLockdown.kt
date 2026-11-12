package id.sch.mts241alikhlash.istiqra.ui

import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

private object NoTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit

    override fun hide() = Unit
}

private val InvisibleSelection =
    TextSelectionColors(
        handleColor = Color.Transparent,
        backgroundColor = Color.Transparent,
    )

@Composable
fun ExamInputLockdown(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTextToolbar provides NoTextToolbar,
        LocalTextSelectionColors provides InvisibleSelection,
        content = content,
    )
}
