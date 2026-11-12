package id.sch.mts241alikhlash.istiqra.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncBackoffTest {
    @Test
    fun `each failure doubles the wait up to the cap`() {
        assertEquals(1_000L, SyncBackoff.delayMs(1, 0.5))
        assertEquals(2_000L, SyncBackoff.delayMs(2, 0.5))
        assertEquals(4_000L, SyncBackoff.delayMs(3, 0.5))
        assertEquals(8_000L, SyncBackoff.delayMs(4, 0.5))
        assertEquals(16_000L, SyncBackoff.delayMs(5, 0.5))
        assertEquals(SyncBackoff.MAX_MS, SyncBackoff.delayMs(9, 0.5))
    }

    @Test
    fun `jitter spreads retries around the base wait`() {
        val low = SyncBackoff.delayMs(3, 0.0)
        val high = SyncBackoff.delayMs(3, 1.0)
        assertEquals(3_200L, low)
        assertEquals(4_800L, high)
        assertTrue(low < SyncBackoff.delayMs(3, 0.5))
        assertTrue(high > SyncBackoff.delayMs(3, 0.5))
    }

    @Test
    fun `a room that fails together does not retry in lockstep`() {
        val waits = (0..9).map { SyncBackoff.delayMs(2, it / 10.0) }
        assertTrue(waits.toSet().size > 1)
        assertTrue(waits.all { it in 1_600L..2_400L })
    }
}
