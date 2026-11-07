package id.sch.mts241alikhlash.exam.kiosk

import id.sch.mts241alikhlash.exam.data.local.CachedSessionEventEntity
import id.sch.mts241alikhlash.exam.data.local.SessionEventDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionEventTrackerTest {
    private class FakeDao : SessionEventDao {
        val rows = mutableListOf<CachedSessionEventEntity>()

        override suspend fun insert(event: CachedSessionEventEntity) {
            rows += event
        }

        override suspend fun unsynced(sessionId: String): List<CachedSessionEventEntity> = rows.filter { it.sessionId == sessionId && !it.isSynced }

        override suspend fun markSynced(clientIds: List<String>) {
            rows.replaceAll { if (it.clientId in clientIds) it.copy(isSynced = true) else it }
        }

        override suspend fun prune(sessionId: String) {
            rows.removeAll { it.isSynced && it.sessionId != sessionId }
        }
    }

    private val dao = FakeDao()
    private val guard = InternalIntentGuard()
    private var ids = 0

    private fun TestScope.tracker() =
        SessionEventTracker(
            dao = dao,
            sessionId = "session-1",
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            guard = guard,
            clientId = { "client-${++ids}" },
            nowMs = { 42_000L },
            writeDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

    @Test
    fun `stopping while the guard is armed logs a permission dialog and no violation`() =
        runTest {
            val tracker = tracker()
            guard.markLaunchingInternalDialog()

            tracker.onActivityStopped(isScreenInteractive = true, nowMs = 1_000)
            runCurrent()

            val row = dao.rows.single()
            assertEquals(SessionEventType.PERMISSION_DIALOG.name, row.type)
            assertFalse(row.isViolation)
            assertEquals(0, dao.rows.count { it.isViolation })
        }

    @Test
    fun `focus returning before the threshold logs a brief focus loss`() =
        runTest {
            val tracker = tracker()

            tracker.onWindowFocusChanged(hasFocus = false, nowMs = 5_000)
            tracker.onWindowFocusChanged(
                hasFocus = true,
                nowMs = 5_000 + SessionEventTracker.FOCUS_THRESHOLD_MS - 1,
            )
            runCurrent()

            val row = dao.rows.single()
            assertEquals(SessionEventType.FOCUS_LOST_BRIEF.name, row.type)
            assertFalse(row.isViolation)
        }

    @Test
    fun `focus returning after the threshold logs an overlay warning`() =
        runTest {
            val tracker = tracker()

            tracker.onWindowFocusChanged(hasFocus = false, nowMs = 5_000)
            tracker.onWindowFocusChanged(
                hasFocus = true,
                nowMs = 5_000 + SessionEventTracker.FOCUS_THRESHOLD_MS,
            )
            runCurrent()

            val row = dao.rows.single()
            assertEquals(SessionEventType.OVERLAY_OR_SHADE.name, row.type)
            assertFalse(row.isViolation)
            assertEquals(0, dao.rows.count { it.isViolation })
        }

    @Test
    fun `a keyboard covering the window is not an overlay`() =
        runTest {
            val tracker = tracker()

            tracker.onCoveringWindowChanged(covering = true, nowMs = 1_000)
            tracker.onWindowFocusChanged(hasFocus = false, nowMs = 2_000)
            advanceTimeBy(SessionEventTracker.FOCUS_THRESHOLD_MS * 2)
            tracker.onCoveringWindowChanged(covering = false, nowMs = 30_000)
            tracker.onWindowFocusChanged(hasFocus = true, nowMs = 31_000)
            runCurrent()

            assertEquals(emptyList<String>(), dao.rows.map { it.type })
        }

    @Test
    fun `the watchdog records one overlay warning when focus never returns`() =
        runTest {
            val tracker = tracker()

            tracker.onWindowFocusChanged(hasFocus = false, nowMs = 0)
            advanceTimeBy(SessionEventTracker.FOCUS_THRESHOLD_MS)
            runCurrent()

            val row = dao.rows.single()
            assertEquals(SessionEventType.OVERLAY_OR_SHADE.name, row.type)
            assertFalse(row.isViolation)
            assertEquals(42_000L, row.occurredAtMs)
        }

    @Test
    fun `an unguarded stop while the screen is on logs leaving the app`() =
        runTest {
            val tracker = tracker()

            tracker.onActivityStopped(isScreenInteractive = true, nowMs = 9_000)
            runCurrent()

            val row = dao.rows.single()
            assertEquals(SessionEventType.LEFT_APP.name, row.type)
            assertTrue(row.isViolation)
            assertEquals("session-1", row.sessionId)
            assertEquals(9_000L, row.occurredAtMs)
            assertEquals("client-1", row.clientId)
            assertTrue(tracker.requiresTokenReentry.value)
        }

    @Test
    fun `an unguarded stop while the screen is off logs a screen off event`() =
        runTest {
            val tracker = tracker()

            tracker.onActivityStopped(isScreenInteractive = false, nowMs = 9_000)
            runCurrent()

            val row = dao.rows.single()
            assertEquals(SessionEventType.SCREEN_OFF.name, row.type)
            assertFalse(row.isViolation)
            assertFalse(tracker.requiresTokenReentry.value)
        }

    @Test
    fun `an active call or microphone logs once until it clears`() =
        runTest {
            val tracker = tracker()

            tracker.onCallOrMicChanged(active = true, nowMs = 1_000)
            tracker.onCallOrMicChanged(active = true, nowMs = 2_000)
            tracker.onCallOrMicChanged(active = false, nowMs = 3_000)
            tracker.onCallOrMicChanged(active = true, nowMs = 4_000)
            runCurrent()

            assertEquals(
                listOf(
                    SessionEventType.CALL_OR_MIC_ACTIVE.name,
                    SessionEventType.CALL_OR_MIC_ACTIVE.name,
                ),
                dao.rows.map { it.type },
            )
            assertTrue(dao.rows.none { it.isViolation })
        }

    @Test
    fun `a secondary display logs a screen share violation once`() =
        runTest {
            val tracker = tracker()

            tracker.onSecondaryDisplayChanged(active = true, nowMs = 1_000)
            tracker.onSecondaryDisplayChanged(active = true, nowMs = 2_000)
            runCurrent()

            val row = dao.rows.single()
            assertEquals(SessionEventType.SCREEN_SHARE_DETECTED.name, row.type)
            assertTrue(row.isViolation)
        }

    @Test
    fun `stopping right after a focus loss does not double count`() =
        runTest {
            val tracker = tracker()

            tracker.onWindowFocusChanged(hasFocus = false, nowMs = 1_000)
            tracker.onActivityStopped(isScreenInteractive = true, nowMs = 1_500)
            tracker.onWindowFocusChanged(hasFocus = true, nowMs = 4_500)
            runCurrent()

            assertEquals(listOf(SessionEventType.LEFT_APP.name), dao.rows.map { it.type })
        }

    @Test
    fun `a late stop after the watchdog fired does not add a second event`() =
        runTest {
            val tracker = tracker()

            tracker.onWindowFocusChanged(hasFocus = false, nowMs = 1_000)
            advanceTimeBy(SessionEventTracker.FOCUS_THRESHOLD_MS)
            runCurrent()
            tracker.onActivityStopped(isScreenInteractive = true, nowMs = 4_500)
            runCurrent()

            assertEquals(listOf(SessionEventType.OVERLAY_OR_SHADE.name), dao.rows.map { it.type })
            assertEquals(0, dao.rows.count { it.isViolation })
        }

    @Test
    fun `a screen off stop after the watchdog still logs screen off without requiring a token`() =
        runTest {
            val tracker = tracker()

            tracker.onWindowFocusChanged(hasFocus = false, nowMs = 1_000)
            advanceTimeBy(SessionEventTracker.FOCUS_THRESHOLD_MS)
            runCurrent()
            tracker.onActivityStopped(isScreenInteractive = false, nowMs = 4_500)
            runCurrent()

            assertEquals(
                listOf(
                    SessionEventType.OVERLAY_OR_SHADE.name,
                    SessionEventType.SCREEN_OFF.name,
                ),
                dao.rows.map { it.type },
            )
            assertFalse(dao.rows.last().isViolation)
            assertFalse(tracker.requiresTokenReentry.value)
        }

    @Test
    fun `closing the tracker stops further writes`() =
        runTest {
            val tracker = tracker()

            tracker.close()
            tracker.onCallOrMicChanged(active = true, nowMs = 1_000)
            runCurrent()

            assertTrue(dao.rows.isEmpty())
        }

    @Test
    fun `the guard reports and clears a single internal launch`() {
        val guard = InternalIntentGuard()
        assertFalse(guard.isGuarded)

        guard.markLaunchingInternalDialog()
        assertTrue(guard.isGuarded)
        assertTrue(guard.consumeOnResume())
        assertFalse(guard.isGuarded)
        assertFalse(guard.consumeOnResume())
    }
}
