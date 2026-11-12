package id.sch.mts241alikhlash.exam.data.session

import id.sch.mts241alikhlash.exam.api.exam.models.ExamPolicyDto

class SessionRepository(
    private val store: SessionStore,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun current(): String? = store.load()

    suspend fun deadline(): Long? = store.loadDeadline()

    suspend fun start(
        id: String,
        deadline: Long = ExamTimer.deadlineFrom(now()),
        policy: ExamPolicyDto? = null,
    ) {
        val trimmed = id.trim()
        require(trimmed.isNotEmpty()) { "Session id must not be blank" }

        if (policy != null) store.savePolicy(policy)

        val existingId = store.load()
        val existingDeadline = store.loadDeadline()
        if (existingId == trimmed && existingDeadline != null) {
            return
        }

        store.save(trimmed, deadline)
    }

    suspend fun applyStoredPolicy() {
        store.loadPolicy()?.let(ExamRuntimePolicy::apply)
    }

    suspend fun reentryPending(id: String): Boolean = store.load() == id && store.loadReentryPending()

    suspend fun markReentryPending(id: String) {
        if (store.load() != id) return
        store.saveReentryPending(true)
    }

    suspend fun clearReentryPending(id: String) {
        if (store.load() != id) return
        store.saveReentryPending(false)
    }

    suspend fun retarget(
        id: String,
        deadline: Long,
    ) {
        if (store.load() != id) return
        store.save(id, deadline)
    }

    suspend fun finish() {
        store.clear()
    }

    suspend fun finishIfCurrent(sessionId: String?): Boolean {
        if (sessionId == null) return false
        if (store.load() != sessionId) return false
        store.clear()
        return true
    }
}
