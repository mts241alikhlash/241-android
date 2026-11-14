package id.sch.mts241alikhlash.istiqra.kiosk

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ExamKeysTest {
    @Test
    fun blocksTheVolumeAndBackKeysWhileTheExamRuns() {
        assertEquals(true, blocksExamKey(true, KeyEvent.KEYCODE_VOLUME_UP))
        assertEquals(true, blocksExamKey(true, KeyEvent.KEYCODE_VOLUME_DOWN))
        assertEquals(true, blocksExamKey(true, KeyEvent.KEYCODE_VOLUME_MUTE))
        assertEquals(true, blocksExamKey(true, KeyEvent.KEYCODE_BACK))
    }

    @Test
    fun letsEveryOtherKeyThrough() {
        assertEquals(false, blocksExamKey(true, KeyEvent.KEYCODE_A))
        assertEquals(false, blocksExamKey(true, KeyEvent.KEYCODE_ENTER))
    }

    @Test
    fun leavesTheKeysAloneOutsideTheExam() {
        assertEquals(false, blocksExamKey(false, KeyEvent.KEYCODE_VOLUME_UP))
        assertEquals(false, blocksExamKey(false, KeyEvent.KEYCODE_BACK))
    }

    @Test
    fun blocksCopyAndPasteShortcutsFromAPhysicalKeyboard() {
        assertEquals(true, blocksExamShortcut(true, KeyEvent.KEYCODE_V, ctrl = true, meta = false))
        assertEquals(true, blocksExamShortcut(true, KeyEvent.KEYCODE_C, ctrl = false, meta = true))
        assertEquals(true, blocksExamShortcut(true, KeyEvent.KEYCODE_PASTE, ctrl = false, meta = false))
        assertEquals(true, blocksExamShortcut(true, KeyEvent.KEYCODE_COPY, ctrl = false, meta = false))
        assertEquals(true, blocksExamShortcut(true, KeyEvent.KEYCODE_CUT, ctrl = false, meta = false))
    }

    @Test
    fun letsPlainTypingFromAPhysicalKeyboardThrough() {
        assertEquals(false, blocksExamShortcut(true, KeyEvent.KEYCODE_V, ctrl = false, meta = false))
        assertEquals(false, blocksExamShortcut(false, KeyEvent.KEYCODE_V, ctrl = true, meta = false))
    }
}
