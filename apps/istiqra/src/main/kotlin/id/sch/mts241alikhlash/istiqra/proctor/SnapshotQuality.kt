package id.sch.mts241alikhlash.istiqra.proctor

object SnapshotQuality {
    const val MAX_LONG_SIDE = 640
    const val MAX_BYTES = 1_000_000
    val LADDER = intArrayOf(70, 50, 35)

    fun nextQuality(
        current: Int,
        bytes: Int,
        limit: Int,
    ): Int? {
        if (bytes <= limit) return null
        return LADDER.firstOrNull { it < current }
    }
}
