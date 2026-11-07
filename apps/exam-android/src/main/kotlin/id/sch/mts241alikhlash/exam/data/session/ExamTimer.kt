package id.sch.mts241alikhlash.exam.data.session

object ExamTimer {
    const val EXAM_DURATION_SECONDS: Long = 90L * 60L
    const val EXAM_DURATION_MS: Long = EXAM_DURATION_SECONDS * 1000L

    const val RESUME_TOLERANCE_SECONDS: Long = 2L

    fun deadlineFrom(startedAt: Long): Long = startedAt + EXAM_DURATION_MS

    fun remainingSeconds(
        deadline: Long,
        now: Long,
    ): Long {
        val millisLeft = (deadline - now).coerceAtLeast(0L)
        return (millisLeft + 999L) / 1000L
    }

    fun isResumed(
        deadline: Long,
        now: Long,
        restored: Boolean? = null,
    ): Boolean = restored ?: (remainingSeconds(deadline, now) < EXAM_DURATION_SECONDS - RESUME_TOLERANCE_SECONDS)

    fun secondsUntilSubmitAllowed(
        startedAtIso: String,
        minSubmitMinutes: Int,
        deadlineIso: String,
        nowMillis: Long,
    ): Long =
        secondsUntilSubmitAllowed(
            parseIsoMillis(startedAtIso),
            minSubmitMinutes,
            parseIsoMillis(deadlineIso),
            nowMillis,
        )

    fun secondsUntilSubmitAllowed(
        startedAtMillis: Long,
        minSubmitMinutes: Int,
        deadlineMillis: Long,
        nowMillis: Long,
    ): Long {
        if (minSubmitMinutes <= 0) return 0L
        val earliestSubmitMillis =
            minOf(
                startedAtMillis + minSubmitMinutes * 60_000L,
                deadlineMillis,
            )
        return ((earliestSubmitMillis - nowMillis) / 1000L).coerceAtLeast(0L)
    }
}
