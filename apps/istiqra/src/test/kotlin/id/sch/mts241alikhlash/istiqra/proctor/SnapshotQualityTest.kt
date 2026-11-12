package id.sch.mts241alikhlash.istiqra.proctor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotQualityTest {
    @Test
    fun `a jpeg inside the limit keeps its quality`() {
        assertNull(SnapshotQuality.nextQuality(70, 900_000, 1_000_000))
    }

    @Test
    fun `the limit itself is not a reason to step down`() {
        assertNull(SnapshotQuality.nextQuality(70, 1_000_000, 1_000_000))
    }

    @Test
    fun `a jpeg over the limit walks the ladder and then gives up`() {
        val over = 1_000_001

        assertEquals(50, SnapshotQuality.nextQuality(70, over, 1_000_000))
        assertEquals(35, SnapshotQuality.nextQuality(50, over, 1_000_000))
        assertNull(SnapshotQuality.nextQuality(35, over, 1_000_000))
    }
}
