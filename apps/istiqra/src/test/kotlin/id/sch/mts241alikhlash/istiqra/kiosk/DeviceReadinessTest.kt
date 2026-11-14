package id.sch.mts241alikhlash.istiqra.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceReadinessTest {
    @Test
    fun `all three checks passing allows the student to proceed`() {
        val state = DeviceReadinessState(dndGranted = true, audioClear = true, online = true)

        assertTrue(state.allReady)
        assertTrue(state.blockingReasons().isEmpty())
    }

    @Test
    fun `every missing check blocks the student`() {
        val blockers =
            listOf(
                ReadinessBlocker.DND to DeviceReadinessState(dndGranted = false, audioClear = true, online = true),
                ReadinessBlocker.CALL_OR_MIC to DeviceReadinessState(dndGranted = true, audioClear = false, online = true),
                ReadinessBlocker.OFFLINE to DeviceReadinessState(dndGranted = true, audioClear = true, online = false),
            )

        blockers.forEach { (expected, state) ->
            assertFalse(state.allReady)
            assertEquals(listOf(expected), state.blockingReasons())
        }
    }

    @Test
    fun `blockers list every failing check in order`() {
        val state = DeviceReadinessState(dndGranted = false, audioClear = true, online = false)

        assertFalse(state.allReady)
        assertEquals(listOf(ReadinessBlocker.DND, ReadinessBlocker.OFFLINE), state.blockingReasons())
    }

    @Test
    fun `default state is not ready`() {
        val state = DeviceReadinessState()

        assertFalse(state.allReady)
        assertEquals(listOf(ReadinessBlocker.DND, ReadinessBlocker.OFFLINE), state.blockingReasons())
    }

    @Test
    fun `a keyboard from outside the system blocks the student`() {
        val state = DeviceReadinessState(dndGranted = true, audioClear = true, online = true, keyboardTrusted = false)

        assertFalse(state.allReady)
        assertEquals(listOf(ReadinessBlocker.KEYBOARD), state.blockingReasons())
    }

    @Test
    fun `a keyboard is trusted only when it ships with the system`() {
        assertTrue(isTrustedKeyboard(systemApp = true, updatedSystemApp = false))
        assertTrue(isTrustedKeyboard(systemApp = false, updatedSystemApp = true))
        assertFalse(isTrustedKeyboard(systemApp = false, updatedSystemApp = false))
    }
}
