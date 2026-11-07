package id.sch.mts241alikhlash.exam.kiosk

import java.util.UUID

data class PendingSuspension(
    val clientId: String,
    val seconds: Int,
)

class CallSuspension(
    private val clientId: () -> String = { UUID.randomUUID().toString() },
) {
    private var startedAtMs: Long? = null
    private var pendingClientId: String? = null

    val active: Boolean get() = startedAtMs != null

    fun begin(nowMs: Long): Boolean {
        if (startedAtMs != null) return false
        startedAtMs = nowMs
        pendingClientId = clientId()
        return true
    }

    fun end(nowMs: Long): PendingSuspension? {
        val startedAt = startedAtMs ?: return null
        val id = pendingClientId ?: return null
        startedAtMs = null
        return PendingSuspension(id, suspendSeconds(nowMs - startedAt))
    }

    companion object {
        const val MIN_SECONDS = 1
        const val MAX_SECONDS = 900

        fun suspendSeconds(elapsedMs: Long): Int = ((elapsedMs + 999L) / 1000L).coerceIn(MIN_SECONDS.toLong(), MAX_SECONDS.toLong()).toInt()
    }
}
