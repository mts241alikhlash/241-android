package id.sch.mts241alikhlash.exam.kiosk

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Test

class ExamKeysTest {
    @Test
    fun blocksTheVolumeKeysWhileTheExamRuns() {
        assertEquals(true, blocksExamKey(true, KeyEvent.KEYCODE_VOLUME_UP))
        assertEquals(true, blocksExamKey(true, KeyEvent.KEYCODE_VOLUME_DOWN))
        assertEquals(true, blocksExamKey(true, KeyEvent.KEYCODE_VOLUME_MUTE))
    }

    @Test
    fun letsEveryOtherKeyThrough() {
        assertEquals(false, blocksExamKey(true, KeyEvent.KEYCODE_BACK))
        assertEquals(false, blocksExamKey(true, KeyEvent.KEYCODE_A))
    }

    @Test
    fun leavesTheVolumeKeysAloneOutsideTheExam() {
        assertEquals(false, blocksExamKey(false, KeyEvent.KEYCODE_VOLUME_UP))
    }
}
