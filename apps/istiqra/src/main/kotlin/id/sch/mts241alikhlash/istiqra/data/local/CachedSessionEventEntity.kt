package id.sch.mts241alikhlash.istiqra.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_events")
data class CachedSessionEventEntity(
    @PrimaryKey val clientId: String,
    val sessionId: String,
    val type: String,
    val isViolation: Boolean,
    val occurredAtMs: Long,
    val detail: String? = null,
    val isSynced: Boolean = false,
)
