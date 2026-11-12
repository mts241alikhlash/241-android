package id.sch.mts241alikhlash.istiqra.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KioskPinRetryPolicyTest {
    @Test
    fun `rejected pin keeps the gate closed and advances the attempt counter`() {
        val policy = KioskPinRetryPolicy()

        val locked = policy.onRetryAttempt(locked = false)

        assertFalse(locked)
        assertEquals(1, policy.pinAttempts)
        assertFalse(policy.gateOpen)
    }

    @Test
    fun `multiple retry failures keep the gate strictly closed`() {
        val policy = KioskPinRetryPolicy()

        policy.onRetryAttempt(locked = false)
        policy.onRetryAttempt(locked = false)
        policy.onRetryAttempt(locked = false)

        assertEquals(3, policy.pinAttempts)
        assertFalse(policy.gateOpen)
    }

    @Test
    fun `a successful pin clears earlier failures and opens the gate`() {
        val policy = KioskPinRetryPolicy()
        policy.onRetryAttempt(locked = false)

        val locked = policy.onRetryAttempt(locked = true)

        assertTrue(locked)
        assertEquals(0, policy.pinAttempts)
        assertTrue(policy.gateOpen)
    }

    @Test
    fun `watchdog refreshes never advance the attempt counter and follow locked state`() {
        val policy = KioskPinRetryPolicy()

        repeat(5) { policy.onStateRefresh(locked = false) }
        assertEquals(0, policy.pinAttempts)
        assertFalse(policy.gateOpen)

        policy.onStateRefresh(locked = true)
        assertEquals(0, policy.pinAttempts)
        assertTrue(policy.gateOpen)

        policy.onStateRefresh(locked = false)
        assertFalse(policy.gateOpen)
    }

    @Test
    fun `reset returns the policy to the pinned mode and closes the gate`() {
        val policy = KioskPinRetryPolicy()
        policy.onRetryAttempt(locked = true)
        assertTrue(policy.gateOpen)

        policy.reset()

        assertEquals("PINNED", policy.kioskMode)
        assertEquals(0, policy.pinAttempts)
        assertFalse(policy.gateOpen)
    }
}
