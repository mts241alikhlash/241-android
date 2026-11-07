package id.sch.mts241alikhlash.exam.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import id.sch.mts241alikhlash.exam.ui.theme.BrandBackground
import id.sch.mts241alikhlash.exam.ui.theme.BrandNavy
import id.sch.mts241alikhlash.exam.ui.theme.BrandSurface
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * R1: the app root must install the brand theme.
 *
 * The screen snapshots cannot catch a missing theme on their own — they can be regenerated
 * blindly. This test renders the real root and reads the colour scheme from *inside* the
 * composition it produces, so dropping `AssessmentTheme` from the root fails on readable
 * values rather than on a pixel diff.
 */
class AppRootThemeTest {
    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig = DeviceConfig.PIXEL_5,
            theme = "android:Theme.Material.NoActionBar",
        )

    @Test
    fun `app root applies the brand colour scheme`() {
        val seen = mutableMapOf<String, Color>()

        paparazzi.snapshot {
            AssessmentAppRoot(
                state = AppRootState(restoreComplete = true, sessionId = "s1"),
                onRetryRestore = {},
                onStartExam = {},
                examContent = { _, _ ->
                    CaptureScheme(seen)
                },
            )
        }

        assertEquals("root must install the brand primary", BrandNavy, seen["primary"])
        assertEquals("labels on a navy button must resolve to white", BrandSurface, seen["onPrimary"])
        assertEquals(
            "scaffold background must come from the brand palette",
            BrandBackground,
            seen["background"],
        )
        assertEquals(
            "navy button labels must not fall back to Unspecified",
            BrandSurface,
            seen["labelOnNavy"],
        )
    }

    /** Reads the scheme the *root* installed, not one this test set up itself. */
    @Composable
    private fun CaptureScheme(into: MutableMap<String, Color>) {
        into["primary"] = MaterialTheme.colorScheme.primary
        into["onPrimary"] = MaterialTheme.colorScheme.onPrimary
        into["background"] = MaterialTheme.colorScheme.background
        // Exactly what ButtonDefaults.buttonColors(containerColor = BrandNavy) resolves
        // for its label; Color.Unspecified without a theme in scope.
        into["labelOnNavy"] = contentColorFor(BrandNavy)
    }
}
