package id.sch.mts241alikhlash.istiqra.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SessionEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: CachedSessionEventEntity)

    @Query("SELECT * FROM session_events WHERE sessionId = :sessionId AND isSynced = 0 ORDER BY occurredAtMs ASC")
    suspend fun unsynced(sessionId: String): List<CachedSessionEventEntity>

    @Query("UPDATE session_events SET isSynced = 1 WHERE clientId IN (:clientIds)")
    suspend fun markSynced(clientIds: List<String>)

    @Query("DELETE FROM session_events WHERE isSynced = 1 AND sessionId != :sessionId")
    suspend fun prune(sessionId: String)
}

object NoSessionEvents : SessionEventDao {
    override suspend fun insert(event: CachedSessionEventEntity) = Unit

    override suspend fun unsynced(sessionId: String): List<CachedSessionEventEntity> = emptyList()

    override suspend fun markSynced(clientIds: List<String>) = Unit

    override suspend fun prune(sessionId: String) = Unit
}
