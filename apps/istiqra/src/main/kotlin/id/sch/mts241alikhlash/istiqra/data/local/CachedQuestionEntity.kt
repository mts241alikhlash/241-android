package id.sch.mts241alikhlash.istiqra.data.local

import androidx.room.Entity

@Entity(
    tableName = "cached_questions",
    primaryKeys = ["sessionId", "id"],
)
data class CachedQuestionEntity(
    val sessionId: String,
    val id: String,
    val number: Int,
    val type: String,
    val bodyHtml: String,
    val points: Double,
    val choicesJson: String?,
    val cachedAt: Long,
)
