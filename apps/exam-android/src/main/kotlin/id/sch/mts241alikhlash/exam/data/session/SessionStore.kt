package id.sch.mts241alikhlash.exam.data.session

interface SessionStore {
    suspend fun load(): String?

    suspend fun loadDeadline(): Long?

    suspend fun save(
        id: String,
        deadline: Long,
    )

    suspend fun clear()

    suspend fun loadReentryPending(): Boolean = false

    suspend fun saveReentryPending(pending: Boolean) = Unit
}
