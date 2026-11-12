package id.sch.mts241alikhlash.istiqra.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import id.sch.mts241alikhlash.istiqra.ui.theme.BrandBackground
import id.sch.mts241alikhlash.istiqra.ui.theme.BrandNavy
import id.sch.mts241alikhlash.istiqra.ui.theme.BrandSurface
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

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

    @Composable
    private fun CaptureScheme(into: MutableMap<String, Color>) {
        into["primary"] = MaterialTheme.colorScheme.primary
        into["onPrimary"] = MaterialTheme.colorScheme.onPrimary
        into["background"] = MaterialTheme.colorScheme.background
        into["labelOnNavy"] = contentColorFor(BrandNavy)
    }
}
