package id.sch.mts241alikhlash.exam.proctor

import org.junit.Assert.assertTrue
import org.junit.Test

class ProctorFlagsTest {
    @Test
    fun `proctor flavor has proctoring enabled`() {
        assertTrue(ProctorFlags.ENABLED)
    }
}
