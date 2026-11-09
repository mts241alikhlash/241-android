package id.sch.mts241alikhlash.exam.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import id.sch.mts241alikhlash.exam.data.model.ExamPolicyDto
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.sessionDataStore by preferencesDataStore(name = "exam_session")

private val JSON = Json { ignoreUnknownKeys = true }

class DataStoreSessionStore(
    private val context: Context,
) : SessionStore {
    override suspend fun load(): String? = context.sessionDataStore.data.first()[SESSION_ID]

    override suspend fun loadDeadline(): Long? = context.sessionDataStore.data.first()[DEADLINE]

    override suspend fun loadReentryPending(): Boolean = context.sessionDataStore.data.first()[REENTRY_PENDING] ?: false

    override suspend fun saveReentryPending(pending: Boolean) {
        context.sessionDataStore.edit { preferences ->
            preferences[REENTRY_PENDING] = pending
        }
    }

    override suspend fun loadPolicy(): ExamPolicyDto? =
        context.sessionDataStore.data.first()[POLICY]?.let { stored ->
            runCatching { JSON.decodeFromString(ExamPolicyDto.serializer(), stored) }.getOrNull()
        }

    override suspend fun savePolicy(policy: ExamPolicyDto) {
        context.sessionDataStore.edit { preferences ->
            preferences[POLICY] = JSON.encodeToString(ExamPolicyDto.serializer(), policy)
        }
    }

    override suspend fun save(
        id: String,
        deadline: Long,
    ) {
        context.sessionDataStore.edit { preferences ->
            preferences[SESSION_ID] = id
            preferences[DEADLINE] = deadline
        }
    }

    override suspend fun clear() {
        context.sessionDataStore.edit { preferences ->
            preferences.remove(SESSION_ID)
            preferences.remove(DEADLINE)
            preferences.remove(REENTRY_PENDING)
            preferences.remove(POLICY)
        }
    }

    private companion object {
        val SESSION_ID = stringPreferencesKey("session_id")
        val DEADLINE = longPreferencesKey("exam_deadline")
        val REENTRY_PENDING = booleanPreferencesKey("requires_token_reentry")
        val POLICY = stringPreferencesKey("exam_policy")
    }
}
