package id.sch.mts241alikhlash.exam.data.sync

object SyncBackoff {
    const val BASE_MS = 1_000L
    const val MAX_MS = 30_000L
    private const val JITTER = 0.2
    private const val MAX_EXPONENT = 6

    fun delayMs(
        failures: Int,
        jitter: Double,
    ): Long {
        val exponent = failures.coerceIn(1, MAX_EXPONENT)
        val base = (BASE_MS shl (exponent - 1)).coerceAtMost(MAX_MS)
        val spread = (base * JITTER).toLong()
        val offset = (spread * (jitter.coerceIn(0.0, 1.0) * 2 - 1)).toLong()
        return (base + offset).coerceIn(BASE_MS, MAX_MS)
    }
}
