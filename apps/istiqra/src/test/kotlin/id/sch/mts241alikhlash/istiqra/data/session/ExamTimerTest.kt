package id.sch.mts241alikhlash.istiqra.data.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamTimerTest {
    private val start = 1_700_000_000_000L

    @Test
    fun `deadline is 90 minutes after start`() {
        assertEquals(start + 5_400_000L, ExamTimer.deadlineFrom(start))
    }

    @Test
    fun `remaining time is derived from the deadline`() {
        val deadline = ExamTimer.deadlineFrom(start)
        assertEquals(5_400L, ExamTimer.remainingSeconds(deadline, start))
        assertEquals(5_100L, ExamTimer.remainingSeconds(deadline, start + 300_000L))
    }

    @Test
    fun `remaining time never goes negative`() {
        val deadline = ExamTimer.deadlineFrom(start)
        assertEquals(0L, ExamTimer.remainingSeconds(deadline, start + 10_000_000L))
    }

    @Test
    fun `sub-second remainders round up so the clock never understates the time`() {
        val deadline = ExamTimer.deadlineFrom(start)
        assertEquals(5_399L, ExamTimer.remainingSeconds(deadline, start + 1_500L))
    }

    @Test
    fun `a fresh session is not marked resumed`() {
        val deadline = ExamTimer.deadlineFrom(start)
        assertFalse(ExamTimer.isResumed(deadline, start))
    }

    @Test
    fun `startup latency does not mark a fresh session as resumed`() {
        val deadline = ExamTimer.deadlineFrom(start)
        val nowAfterStartup = deadline - ExamTimer.EXAM_DURATION_MS + 50L

        assertEquals(
            "a fresh paper must open at 90:00, not 89:59",
            ExamTimer.EXAM_DURATION_SECONDS,
            ExamTimer.remainingSeconds(deadline, nowAfterStartup),
        )
        assertFalse(
            "50 ms of startup latency is not a resumed exam",
            ExamTimer.isResumed(deadline, nowAfterStartup),
        )
    }

    @Test
    fun `sub-second latency anywhere below the tolerance is not resumed`() {
        val deadline = ExamTimer.deadlineFrom(start)
        val justInsideTolerance = deadline - ExamTimer.EXAM_DURATION_MS + 1_900L

        assertFalse(ExamTimer.isResumed(deadline, justInsideTolerance))
    }

    @Test
    fun `a cold start after the clock ran is marked resumed`() {
        val deadline = ExamTimer.deadlineFrom(start)
        assertTrue(ExamTimer.isResumed(deadline, start + 600_000L))
    }

    @Test
    fun `a session resumed by minutes shows the banner`() {
        val deadline = ExamTimer.deadlineFrom(start)
        val now = start + 45L * 60L * 1000L

        assertTrue(ExamTimer.isResumed(deadline, now))
        assertEquals(2_700L, ExamTimer.remainingSeconds(deadline, now))
    }

    @Test
    fun `a session past its tolerance is resumed`() {
        val deadline = ExamTimer.deadlineFrom(start)
        val justPastTolerance = deadline - ExamTimer.EXAM_DURATION_MS + 3_000L

        assertTrue(ExamTimer.isResumed(deadline, justPastTolerance))
    }

    @Test
    fun `explicit not-restored flag wins over the 90 minute heuristic`() {
        val start = 1_700_000_000_000L
        val shortDeadline = start + 60L * 60L * 1000L

        assertFalse(ExamTimer.isResumed(shortDeadline, start, restored = false))
    }

    @Test
    fun `explicit restored flag reports resumed`() {
        val start = 1_700_000_000_000L

        assertTrue(ExamTimer.isResumed(ExamTimer.deadlineFrom(start), start, restored = true))
    }

    @Test
    fun `submit gate counts down to the minimum submit time`() {
        val startedAt = "2026-11-01T01:00:00.000Z"
        val deadline = "2026-11-01T02:30:00.000Z"

        assertEquals(
            900L,
            ExamTimer.secondsUntilSubmitAllowed(startedAt, 20, deadline, 1_793_495_100_000L),
        )
    }

    @Test
    fun `submit gate is zero once the minimum submit time has passed`() {
        val startedAt = "2026-11-01T01:00:00.000Z"
        val deadline = "2026-11-01T02:30:00.000Z"

        assertEquals(
            0L,
            ExamTimer.secondsUntilSubmitAllowed(startedAt, 20, deadline, 1_793_496_200_000L),
        )
    }

    @Test
    fun `submit gate is clamped by the deadline`() {
        val startedAt = "2026-11-01T01:00:00.000Z"
        val deadline = "2026-11-01T01:05:00.000Z"

        assertEquals(
            300L,
            ExamTimer.secondsUntilSubmitAllowed(startedAt, 20, deadline, 1_793_494_800_000L),
        )
    }

    @Test
    fun `submit gate is zero when the schedule has no minimum`() {
        val startedAt = "2026-11-01T01:00:00.000Z"
        val deadline = "2026-11-01T02:30:00.000Z"

        assertEquals(
            0L,
            ExamTimer.secondsUntilSubmitAllowed(startedAt, 0, deadline, 1_793_494_800_000L),
        )
    }
}
