package id.sch.mts241alikhlash.cbt.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

object AnswerSyncState {
    const val LOCAL_PENDING = "LOCAL_PENDING"
    const val SYNCING = "SYNCING"
    const val SERVER_CONFIRMED = "SERVER_CONFIRMED"
}

@Entity(tableName = "cached_answers")
data class CachedAnswerEntity(
    @PrimaryKey val questionId: String,
    val sessionId: String,
    val answer: String,
    val isDoubtful: Boolean = false,
    val clientId: String,
    val syncState: String = AnswerSyncState.LOCAL_PENDING,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val confirmedAtMillis: Long? = null
) {
    val isPending: Boolean get() = syncState != AnswerSyncState.SERVER_CONFIRMED
}
