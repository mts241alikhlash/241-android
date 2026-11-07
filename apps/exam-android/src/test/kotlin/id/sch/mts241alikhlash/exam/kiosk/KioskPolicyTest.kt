package id.sch.mts241alikhlash.exam.kiosk

import android.app.ActivityManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KioskPolicyTest {
    @Test
    fun `pinned mode is secure`() {
        assertTrue(KioskPolicy.isSecure(ActivityManager.LOCK_TASK_MODE_PINNED))
    }

    @Test
    fun `locked mode is secure`() {
        assertTrue(KioskPolicy.isSecure(ActivityManager.LOCK_TASK_MODE_LOCKED))
    }

    @Test
    fun `rejected pin is not secure`() {
        assertFalse(KioskPolicy.isSecure(ActivityManager.LOCK_TASK_MODE_NONE))
    }
}
