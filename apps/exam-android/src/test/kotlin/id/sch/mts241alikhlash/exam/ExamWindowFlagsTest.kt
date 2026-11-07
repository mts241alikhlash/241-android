package id.sch.mts241alikhlash.exam

import android.app.assist.AssistContent
import android.os.Bundle
import android.view.WindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExamWindowFlagsTest {
    @Test
    fun `exam window keeps the screen on`() {
        val activity =
            org.robolectric.Robolectric
                .buildActivity(MainActivity::class.java)
                .setup()
                .get()
        activity.setExamActiveForTest(true)
        val flags = activity.window.attributes.flags
        assertTrue(
            "FLAG_KEEP_SCREEN_ON must be set while the exam UI is active",
            flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0,
        )
        activity.finish()
    }

    @Test
    fun `secure flag is set unconditionally`() {
        val activity =
            org.robolectric.Robolectric
                .buildActivity(MainActivity::class.java)
                .setup()
                .get()
        val flags = activity.window.attributes.flags
        assertTrue(
            "FLAG_SECURE must be set even in debug builds",
            flags and WindowManager.LayoutParams.FLAG_SECURE != 0,
        )
        activity.finish()
    }

    @Test
    fun `assist content and data are withheld`() {
        val activity =
            org.robolectric.Robolectric
                .buildActivity(MainActivity::class.java)
                .setup()
                .get()
        val content = AssistContent()
        activity.onProvideAssistContent(content)
        activity.onProvideAssistData(Bundle())
        assertNull(content.intent)
        assertNull(content.webUri)
        assertNull(content.structuredData)
        activity.finish()
    }

    @Test
    fun `reported count is the store count alone`() {
        val activity =
            org.robolectric.Robolectric
                .buildActivity(MainActivity::class.java)
                .setup()
                .get()

        assertEquals(2, activity.reportedViolationCount(storeCount = 2))
        assertEquals(0, activity.reportedViolationCount(storeCount = 0))
        activity.finish()
    }

    @Test
    fun `screen on flag is released when the exam ends`() {
        val activity =
            org.robolectric.Robolectric
                .buildActivity(MainActivity::class.java)
                .setup()
                .get()
        activity.setExamActiveForTest(false)
        val flags = activity.window.attributes.flags
        assertEquals(
            "KEEP_SCREEN_ON must be cleared after the exam ends",
            0,
            flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        )
        activity.finish()
    }
}
