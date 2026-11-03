package id.sch.mts241alikhlash.cbt.proctor

enum class SnapshotKind {
    LIVE,
    SESSION_START,
    VIOLATION_RETURN,
    PERIODIC_EVIDENCE,
}

class SnapshotScheduler(
    private val isAnswerSyncBusy: () -> Boolean,
) {
    private var ticks = 0

    @Volatile
    private var returnPending = false

    private var retryKind: SnapshotKind? = null

    fun reset() {
        ticks = 0
        returnPending = false
        retryKind = null
    }

    fun onAppReturn() {
        returnPending = true
    }

    fun onUploadFailed(kind: SnapshotKind) {
        if (kind != SnapshotKind.LIVE) retryKind = kind
    }

    fun nextKind(): SnapshotKind? {
        if (isAnswerSyncBusy()) return null

        retryKind?.let {
            retryKind = null
            return it
        }

        ticks++
        if (ticks == 1) {
            returnPending = false
            return SnapshotKind.SESSION_START
        }
        if (ticks % PERIODIC_TICKS == 0) return SnapshotKind.PERIODIC_EVIDENCE
        if (returnPending) {
            returnPending = false
            return SnapshotKind.VIOLATION_RETURN
        }
        return SnapshotKind.LIVE
    }

    companion object {
        const val INTERVAL_MS = 15_000L
        const val PERIODIC_TICKS = 8
    }
}
