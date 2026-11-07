package id.sch.mts241alikhlash.exam.data.session

class SessionRepository(
    private val store: SessionStore,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun current(): String? = store.load()

    suspend fun deadline(): Long? = store.loadDeadline()

    /**
     * Starts [id] with a fresh deadline.
     *
     * Re-starting the session that is already persisted keeps its original deadline:
     * a process restart must not hand the student a brand-new 90 minutes.
     */
    suspend fun start(
        id: String,
        deadline: Long = ExamTimer.deadlineFrom(now()),
    ) {
        val trimmed = id.trim()
        require(trimmed.isNotEmpty()) { "Session id must not be blank" }

        val existingId = store.load()
        val existingDeadline = store.loadDeadline()
        if (existingId == trimmed && existingDeadline != null) {
            return
        }

        store.save(trimmed, deadline)
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

    /**
     * Clears the session only when it is still [sessionId].
     *
     * A teardown that was already in flight when a new exam started must not wipe the
     * new session, so the check and the clear are done against the same read.
     */
    suspend fun finishIfCurrent(sessionId: String?): Boolean {
        if (sessionId == null) return false
        if (store.load() != sessionId) return false
        store.clear()
        return true
    }
}
