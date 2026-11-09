package id.sch.mts241alikhlash.exam.kiosk

import id.sch.mts241alikhlash.exam.data.model.ExamPolicyDto
import id.sch.mts241alikhlash.exam.data.session.ExamRuntimePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViolationPolicyTest {
    @Test
    fun `leaving the app, losing the pin and screen sharing count, overlays only warn`() {
        assertTrue(ViolationPolicy.counts("left_app"))
        assertTrue(ViolationPolicy.counts("pin_lost"))
        assertFalse(ViolationPolicy.counts("overlay_or_shade"))
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
            ViolationPolicy.warning(1),
        )
        assertEquals(
            "Peringatan terakhir (3 dari 3): keluar sekali lagi dan ujian langsung dikumpulkan.",
            ViolationPolicy.warning(3),
        )
        assertEquals(
            "Ujian dikumpulkan karena keluar dari aplikasi lebih dari 3 kali.",
            ViolationPolicy.warning(4),
        )
    }

    @Test
    fun `the activity policy can move the hand-in limit`() {
        assertFalse(ViolationPolicy.exceeded(4, limit = 5))
        assertTrue(ViolationPolicy.exceeded(6, limit = 5))
        assertEquals(
            "Peringatan terakhir (5 dari 5): keluar sekali lagi dan ujian langsung dikumpulkan.",
            ViolationPolicy.warning(5, limit = 5),
        )
    }

    @Test
    fun `the copy follows the stashed runtime limit`() {
        val previous = ExamRuntimePolicy.current
        ExamRuntimePolicy.apply(ExamPolicyDto(violationLimit = 5))

        try {
            assertEquals(5, ViolationPolicy.currentLimit())
            assertEquals(
                "Ujian dikumpulkan karena keluar dari aplikasi lebih dari 5 kali.",
                ViolationPolicy.exitWarning(),
            )
        } finally {
            ExamRuntimePolicy.apply(previous)
        }
    }
}
