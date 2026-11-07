package id.sch.mts241alikhlash.exam.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface QuestionDao {
    @Query("SELECT * FROM cached_questions WHERE sessionId = :sessionId ORDER BY number ASC")
    suspend fun getForSession(sessionId: String): List<CachedQuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedQuestionEntity>)

    @Query("DELETE FROM cached_questions WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    @Transaction
    suspend fun replaceForSession(
        sessionId: String,
        items: List<CachedQuestionEntity>,
    ) {
        deleteForSession(sessionId)
        insertAll(items)
    }
}
