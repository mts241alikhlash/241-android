package id.sch.mts241alikhlash.exam.proctor

import org.junit.Assert.assertFalse
import org.junit.Test

class ProctorFlagsTest {
    @Test
    fun `standard flavor has proctoring disabled`() {
        assertFalse(ProctorFlags.ENABLED)
    }
}
