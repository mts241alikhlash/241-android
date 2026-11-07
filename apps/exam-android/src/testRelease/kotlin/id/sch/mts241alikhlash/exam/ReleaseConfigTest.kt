package id.sch.mts241alikhlash.exam

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseConfigTest {
    @Test
    fun baseUrlUsesHttps() {
        assertTrue(
            "release BASE_URL must be https, was '${BuildConfig.BASE_URL}'",
            BuildConfig.BASE_URL.startsWith("https://"),
        )
    }

    @Test
    fun releaseIsNotDebuggable() {
        assertFalse(BuildConfig.DEBUG)
    }
}
