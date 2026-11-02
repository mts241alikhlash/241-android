package id.sch.mts241alikhlash.cbt.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerClockTest {
    private var elapsed = 10_000L
    private val clock = ServerClock { elapsed }

    @Test
    fun `before the first sync the clock never reports zero`() {
        val now = ServerClock { elapsed }.nowMillis()

        assertTrue(now > 1_600_000_000_000L)
    }

    @Test
    fun `advancing elapsed realtime advances the server clock by the same amount`() {
        clock.syncEpochMillis(1_700_000_000_000L)
        assertEquals(1_700_000_000_000L, clock.nowMillis())

        elapsed += 30_000L

        assertEquals(1_700_000_030_000L, clock.nowMillis())
    }

    @Test
    fun `a wall clock jump does not move the server clock`() {
        clock.syncEpochMillis(1_700_000_000_000L)

        val before = clock.nowMillis()
        elapsed += 1_000L
        val after = clock.nowMillis()

        assertEquals(1_000L, after - before)
    }

    @Test
    fun `syncing again rebases on the new server time`() {
        clock.syncEpochMillis(1_700_000_000_000L)
        elapsed += 5_000L

        clock.syncEpochMillis(1_700_000_100_000L)

        assertEquals(1_700_000_100_000L, clock.nowMillis())
        elapsed += 2_000L
        assertEquals(1_700_000_102_000L, clock.nowMillis())
    }

    @Test
    fun `an iso server time is parsed and then followed monotonically`() {
        clock.sync("2026-11-01T01:00:00.000Z")

        assertEquals(1_793_494_800_000L, clock.nowMillis())

        elapsed += 90_000L

        assertEquals(1_793_494_890_000L, clock.nowMillis())
    }
}
