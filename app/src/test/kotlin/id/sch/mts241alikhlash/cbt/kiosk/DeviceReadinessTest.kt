package id.sch.mts241alikhlash.cbt.kiosk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceReadinessTest {

    @Test
    fun `all four checks passing allows the student to proceed`() {
        val state = DeviceReadinessState(
            dndGranted = true,
            cameraGranted = true,
            audioClear = true,
            online = true
        )

        assertTrue(state.allReady)
        assertTrue(state.blockingReasons().isEmpty())
    }

    @Test
    fun `every missing check blocks the student`() {
        val blockers = listOf(
            ReadinessBlocker.DND to DeviceReadinessState(dndGranted = false, cameraGranted = true, audioClear = true, online = true),
            ReadinessBlocker.CAMERA to DeviceReadinessState(dndGranted = true, cameraGranted = false, audioClear = true, online = true),
            ReadinessBlocker.CALL_OR_MIC to DeviceReadinessState(dndGranted = true, cameraGranted = true, audioClear = false, online = true),
            ReadinessBlocker.OFFLINE to DeviceReadinessState(dndGranted = true, cameraGranted = true, audioClear = true, online = false)
        )

        blockers.forEach { (expected, state) ->
            assertFalse(state.allReady)
            assertEquals(listOf(expected), state.blockingReasons())
        }
    }

    @Test
    fun `default state is not ready`() {
        val state = DeviceReadinessState()

        assertFalse(state.allReady)
        assertEquals(
            listOf(ReadinessBlocker.DND, ReadinessBlocker.CAMERA, ReadinessBlocker.OFFLINE),
            state.blockingReasons()
        )
    }
}
