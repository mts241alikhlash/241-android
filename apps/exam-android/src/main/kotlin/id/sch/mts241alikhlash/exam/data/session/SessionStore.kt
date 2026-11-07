package id.sch.mts241alikhlash.exam.data.session

interface SessionStore {
    suspend fun load(): String?

    /** Absolute epoch millis at which the exam must end, or null when unknown. */
    suspend fun loadDeadline(): Long?

    /** Saves the session id and its deadline in one write so they cannot diverge. */
    suspend fun save(
        id: String,
        deadline: Long,
    )

    suspend fun clear()

    suspend fun loadReentryPending(): Boolean = false

    suspend fun saveReentryPending(pending: Boolean) = Unit
}
