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
}
