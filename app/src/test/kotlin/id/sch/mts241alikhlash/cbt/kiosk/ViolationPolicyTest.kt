package id.sch.mts241alikhlash.cbt.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViolationPolicyTest {

    @Test
    fun `leaving the app, losing the pin, overlays, and screen sharing count`() {
        assertTrue(ViolationPolicy.counts("left_app"))
        assertTrue(ViolationPolicy.counts("pin_lost"))
        assertTrue(ViolationPolicy.counts("overlay_or_shade"))
        assertTrue(ViolationPolicy.counts("screen_share_detected"))
        assertFalse(ViolationPolicy.counts("focus_lost_brief"))
        assertFalse(ViolationPolicy.counts("supervisor_exit"))
        assertFalse(ViolationPolicy.counts("supervisor_exit_denied"))
    }

    @Test
    fun `the exam is handed in on the fourth violation`() {
        assertFalse(ViolationPolicy.exceeded(0))
        assertFalse(ViolationPolicy.exceeded(3))
        assertTrue(ViolationPolicy.exceeded(4))
        assertTrue(ViolationPolicy.exceeded(9))
    }

    @Test
    fun `the warning says how close the student is`() {
        assertEquals(
            "Peringatan 1 dari 3: dilarang keluar dari aplikasi ujian.",
            ViolationPolicy.warning(1)
        )
        assertEquals(
            "Peringatan terakhir (3 dari 3): keluar sekali lagi dan ujian langsung dikumpulkan.",
            ViolationPolicy.warning(3)
        )
        assertEquals(
            "Ujian dikumpulkan karena keluar dari aplikasi lebih dari 3 kali.",
            ViolationPolicy.warning(4)
        )
    }
}
