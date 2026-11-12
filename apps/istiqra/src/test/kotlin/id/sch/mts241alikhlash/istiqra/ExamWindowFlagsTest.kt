package id.sch.mts241alikhlash.istiqra

import android.app.assist.AssistContent
import android.os.Bundle
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import id.sch.mts241alikhlash.istiqra.data.auth.AuthUser
import id.sch.mts241alikhlash.istiqra.kiosk.ViolationStore
import id.sch.mts241alikhlash.istiqra.ui.AppRootState
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
    fun `secure flag follows the build type`() {
        val activity =
            org.robolectric.Robolectric
                .buildActivity(MainActivity::class.java)
                .setup()
                .get()
        assertEquals(
            if (BuildConfig.DEBUG) {
                "FLAG_SECURE is reserved for release builds"
            } else {
                "FLAG_SECURE must be set in release builds"
            },
            if (BuildConfig.DEBUG) 0 else WindowManager.LayoutParams.FLAG_SECURE,
            activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE,
        )
        activity.finish()
    }

    @Test
    fun `releasing the exam returns to the list and resume does not lock again`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.setup().get()
        val store = ViolationStore(ApplicationProvider.getApplicationContext())
        store.beginSession("s1")
        store.record("left_app")
        activity.setExamActiveForTest(true)
        activity.setRootStateForTest(
            AppRootState(
                restoreComplete = true,
                user = AuthUser("u1", "Ahmad", setOf("STUDENT")),
                sessionId = "s1",
                deadline = 1_700_000_000_000L,
                reentrySessionId = "s1",
            ),
        )

        activity.releaseExamForTest("Ujian berhasil dikumpulkan")

        val released = activity.rootStateForTest()
        assertNull(released.sessionId)
        assertNull(released.reentrySessionId)
        assertNull(released.deadline)
        assertEquals("Ujian berhasil dikumpulkan", released.notice)
        assertEquals(0, store.count())
        assertEquals(
            "KEEP_SCREEN_ON must be cleared as soon as the exam ends",
            0,
            activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
        )

        controller.pause().resume()

        assertEquals(
            "resuming a released exam must not lock the screen on again",
            0,
            activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
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
