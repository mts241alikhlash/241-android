package id.sch.mts241alikhlash.istiqra.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallSuspensionTest {
    @Test
    fun `the reported seconds are rounded up and clamped to the contract range`() {
        assertEquals(1, CallSuspension.suspendSeconds(0))
        assertEquals(1, CallSuspension.suspendSeconds(1))
        assertEquals(1, CallSuspension.suspendSeconds(1_000))
        assertEquals(2, CallSuspension.suspendSeconds(1_001))
        assertEquals(900, CallSuspension.suspendSeconds(900_000))
        assertEquals(900, CallSuspension.suspendSeconds(1_800_000))
    }

    @Test
    fun `a pause reports one client id for every retry`() {
        val suspension = CallSuspension { "client-1" }

        assertTrue(suspension.begin(1_000))
        assertFalse(suspension.begin(2_000))
        assertTrue(suspension.active)

        assertEquals(PendingSuspension("client-1", 90), suspension.end(91_000))
        assertNull(suspension.end(91_000))
        assertFalse(suspension.active)
    }

    @Test
    fun `an end without a start reports nothing`() {
        assertNull(CallSuspension().end(1_000))
    }
}
