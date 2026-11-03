package id.sch.mts241alikhlash.cbt.proctor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotSchedulerTest {

    private var busy = false
    private val scheduler = SnapshotScheduler(isAnswerSyncBusy = { busy })

    @Test
    fun `the first tick is the session start`() {
        assertEquals(SnapshotKind.SESSION_START, scheduler.nextKind())
    }

    @Test
    fun `ticks after the start are live`() {
        scheduler.nextKind()
        assertEquals(SnapshotKind.LIVE, scheduler.nextKind())
        assertEquals(SnapshotKind.LIVE, scheduler.nextKind())
    }

    @Test
    fun `every fortieth tick is periodic evidence`() {
        repeat(39) { scheduler.nextKind() }
        assertEquals(SnapshotKind.PERIODIC_EVIDENCE, scheduler.nextKind())
        assertEquals(SnapshotKind.LIVE, scheduler.nextKind())
        repeat(38) { scheduler.nextKind() }
        assertEquals(SnapshotKind.PERIODIC_EVIDENCE, scheduler.nextKind())
    }

    @Test
    fun `the tick before the fortieth stays live`() {
        repeat(38) { scheduler.nextKind() }
        assertEquals(SnapshotKind.LIVE, scheduler.nextKind())
    }

    @Test
    fun `returning to the app asks for violation evidence once`() {
        scheduler.nextKind()
        scheduler.onAppReturn()
        assertEquals(SnapshotKind.VIOLATION_RETURN, scheduler.nextKind())
        assertEquals(SnapshotKind.LIVE, scheduler.nextKind())
    }

    @Test
    fun `a return before the first tick still starts the session`() {
        scheduler.onAppReturn()
        assertEquals(SnapshotKind.SESSION_START, scheduler.nextKind())
    }

    @Test
    fun `a busy answer sync skips the tick without consuming it`() {
        busy = true
        assertNull(scheduler.nextKind())
        busy = false
        assertEquals(SnapshotKind.SESSION_START, scheduler.nextKind())
    }

    @Test
    fun `a busy answer sync keeps the pending return for the next tick`() {
        scheduler.nextKind()
        scheduler.onAppReturn()
        busy = true
        assertNull(scheduler.nextKind())
        busy = false
        assertEquals(SnapshotKind.VIOLATION_RETURN, scheduler.nextKind())
    }

    @Test
    fun `periodic evidence outranks a plain live tick`() {
        repeat(39) { scheduler.nextKind() }
        busy = true
        assertNull(scheduler.nextKind())
        busy = false
        assertEquals(SnapshotKind.PERIODIC_EVIDENCE, scheduler.nextKind())
    }

    @Test
    fun `the cadence is fifteen seconds and eight ticks`() {
        assertEquals(15_000L, SnapshotScheduler.INTERVAL_MS)
        assertEquals(8, SnapshotScheduler.PERIODIC_TICKS)
    }

    @Test
    fun `a failed evidence upload is retried before the next live tick`() {
        assertEquals(SnapshotKind.SESSION_START, scheduler.nextKind())
        scheduler.onUploadFailed(SnapshotKind.SESSION_START)
        assertEquals(SnapshotKind.SESSION_START, scheduler.nextKind())
        assertEquals(SnapshotKind.LIVE, scheduler.nextKind())
    }

    @Test
    fun `a failed violation upload keeps the return for the next tick`() {
        scheduler.nextKind()
        scheduler.onAppReturn()
        assertEquals(SnapshotKind.VIOLATION_RETURN, scheduler.nextKind())
        scheduler.onUploadFailed(SnapshotKind.VIOLATION_RETURN)
        assertEquals(SnapshotKind.VIOLATION_RETURN, scheduler.nextKind())
    }

    @Test
    fun `a failed live upload is not retried`() {
        scheduler.nextKind()
        scheduler.onUploadFailed(SnapshotKind.LIVE)
        assertEquals(SnapshotKind.LIVE, scheduler.nextKind())
        assertEquals(SnapshotKind.LIVE, scheduler.nextKind())
    }

    @Test
    fun `a failed periodic upload is retried and does not shift the cycle`() {
        repeat(39) { scheduler.nextKind() }
        assertEquals(SnapshotKind.PERIODIC_EVIDENCE, scheduler.nextKind())
        scheduler.onUploadFailed(SnapshotKind.PERIODIC_EVIDENCE)
        assertEquals(SnapshotKind.PERIODIC_EVIDENCE, scheduler.nextKind())
        assertEquals(SnapshotKind.LIVE, scheduler.nextKind())
    }

    @Test
    fun `a busy answer sync also defers the retry`() {
        scheduler.nextKind()
        scheduler.onAppReturn()
        scheduler.nextKind()
        scheduler.onUploadFailed(SnapshotKind.VIOLATION_RETURN)
        busy = true
        assertNull(scheduler.nextKind())
        busy = false
        assertEquals(SnapshotKind.VIOLATION_RETURN, scheduler.nextKind())
    }

    @Test
    fun `periodic evidence is not skipped by a pending return`() {
        repeat(39) { scheduler.nextKind() }
        scheduler.onAppReturn()
        assertEquals(SnapshotKind.PERIODIC_EVIDENCE, scheduler.nextKind())
        assertEquals(SnapshotKind.VIOLATION_RETURN, scheduler.nextKind())
    }

    @Test
    fun `a return before the first tick is discarded by the session start`() {
        scheduler.onAppReturn()
        assertEquals(SnapshotKind.SESSION_START, scheduler.nextKind())
        assertEquals(SnapshotKind.LIVE, scheduler.nextKind())
    }
}
