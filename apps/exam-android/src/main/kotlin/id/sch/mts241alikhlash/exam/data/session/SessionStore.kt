package id.sch.mts241alikhlash.exam.data.session

import id.sch.mts241alikhlash.exam.api.exam.models.ExamPolicyDto

interface SessionStore {
    suspend fun load(): String?

    suspend fun loadDeadline(): Long?

    suspend fun save(
        id: String,
        deadline: Long,
    )

    suspend fun clear()

    suspend fun loadPolicy(): ExamPolicyDto? = null

    suspend fun savePolicy(policy: ExamPolicyDto) = Unit

    suspend fun loadReentryPending(): Boolean = false

    suspend fun saveReentryPending(pending: Boolean) = Unit
}
