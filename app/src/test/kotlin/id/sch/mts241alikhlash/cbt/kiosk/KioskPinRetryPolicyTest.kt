package id.sch.mts241alikhlash.cbt.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KioskPinRetryPolicyTest {

    @Test
    fun `first rejected pin keeps the blocking overlay`() {
        val policy = KioskPinRetryPolicy()

        val locked = policy.onRetryAttempt(locked = false)

        assertFalse(locked)
        assertEquals(1, policy.pinAttempts)
        assertFalse(policy.canProceedUnpinned)
        assertFalse(policy.isUnpinned)
        assertFalse(policy.gateOpen)
    }

    @Test
    fun `one retry counts exactly one attempt and keeps the gate closed`() {
        val policy = KioskPinRetryPolicy()

        policy.onRetryAttempt(locked = false)

        assertEquals(1, policy.pinAttempts)
        assertFalse(policy.canProceedUnpinned)
        assertFalse(policy.gateOpen)
    }

    @Test
    fun `second rejected pin unlocks the strict unpinned path`() {
        val policy = KioskPinRetryPolicy()

        policy.onRetryAttempt(locked = false)
        val locked = policy.onRetryAttempt(locked = false)

        assertFalse(locked)
        assertEquals(2, policy.pinAttempts)
        assertTrue(policy.canProceedUnpinned)
        assertFalse(policy.isUnpinned)
    }

    @Test
    fun `choosing strict unpinned mode clears the blocking state`() {
        val policy = KioskPinRetryPolicy()
        policy.onRetryAttempt(locked = false)
        policy.onRetryAttempt(locked = false)

        policy.proceedUnpinned()

        assertEquals("STRICT_UNPINNED", policy.kioskMode)
        assertTrue(policy.isUnpinned)
        assertFalse(policy.canProceedUnpinned)
    }

    @Test
    fun `a successful pin clears earlier failures`() {
        val policy = KioskPinRetryPolicy()
        policy.onRetryAttempt(locked = false)

        val locked = policy.onRetryAttempt(locked = true)

        assertTrue(locked)
        assertEquals(0, policy.pinAttempts)
        assertFalse(policy.canProceedUnpinned)
    }

    @Test
    fun `watchdog refreshes never advance the attempt counter`() {
        val policy = KioskPinRetryPolicy()

        repeat(10) { policy.onStateRefresh(locked = false) }

        assertEquals(0, policy.pinAttempts)
        assertFalse(policy.canProceedUnpinned)
        assertFalse(policy.gateOpen)
    }

    @Test
    fun `only a user retry opens the gate unpinned`() {
        val policy = KioskPinRetryPolicy()
        policy.onStateRefresh(locked = false)

        policy.onRetryAttempt(locked = false)
        assertFalse(policy.gateOpen)

        policy.onRetryAttempt(locked = false)
        assertTrue(policy.canProceedUnpinned)
        assertFalse(policy.gateOpen)

        policy.proceedUnpinned()
        assertTrue(policy.gateOpen)
    }

    @Test
    fun `reset returns the policy to the pinned mode`() {
        val policy = KioskPinRetryPolicy()
        policy.onRetryAttempt(locked = false)
        policy.onRetryAttempt(locked = false)
        policy.proceedUnpinned()

        policy.reset()

        assertEquals("PINNED", policy.kioskMode)
        assertEquals(0, policy.pinAttempts)
        assertFalse(policy.isUnpinned)
        assertFalse(policy.canProceedUnpinned)
    }
}
