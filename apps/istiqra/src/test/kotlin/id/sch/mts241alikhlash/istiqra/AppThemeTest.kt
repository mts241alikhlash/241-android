package id.sch.mts241alikhlash.istiqra

import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "night")
class AppThemeTest {
    private fun themed() = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_Istiqra)

    @Test
    fun theAppThemeStaysLightOnAPhoneInDarkMode() {
        val attrs = themed().obtainStyledAttributes(intArrayOf(android.R.attr.isLightTheme))
        val light = attrs.getBoolean(0, false)
        attrs.recycle()

        assertTrue(light)
    }

    @Test
    fun theSystemMayNotForceTheAppDark() {
        val attrs = themed().obtainStyledAttributes(intArrayOf(android.R.attr.forceDarkAllowed))
        val allowed = attrs.getBoolean(0, true)
        attrs.recycle()

        assertFalse(allowed)
    }
}
