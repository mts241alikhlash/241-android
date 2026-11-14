package id.sch.mts241alikhlash.istiqra.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import id.sch.mts241alikhlash.istiqra.kiosk.blocksExamShortcut

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
    ) {
        Box(
            modifier =
                Modifier.onPreviewKeyEvent { event ->
                    blocksExamShortcut(true, event.nativeKeyEvent.keyCode, event.isCtrlPressed, event.isMetaPressed)
                },
        ) {
            content()
        }
    }
}
