package id.sch.mts241alikhlash.cbt.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {
    @Query("SELECT * FROM cached_answers WHERE sessionId = :sessionId")
    fun observeAnswers(sessionId: String): Flow<List<CachedAnswerEntity>>

    @Query("SELECT * FROM cached_answers WHERE sessionId = :sessionId AND questionId = :questionId LIMIT 1")
    suspend fun getAnswer(sessionId: String, questionId: String): CachedAnswerEntity?

    @Query(
        """
        SELECT * FROM cached_answers
        WHERE sessionId = :sessionId AND syncState != 'SERVER_CONFIRMED'
        ORDER BY updatedAtMillis ASC
        """
    )
    suspend fun getPendingAnswers(sessionId: String): List<CachedAnswerEntity>

    @Query("SELECT COUNT(*) FROM cached_answers WHERE sessionId = :sessionId AND syncState != 'SERVER_CONFIRMED'")
    suspend fun countPendingAnswers(sessionId: String): Int

    @Query("SELECT COUNT(*) FROM cached_answers WHERE sessionId = :sessionId AND answer != ''")
    suspend fun countAnswered(sessionId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAnswer(answer: CachedAnswerEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(answer: CachedAnswerEntity): Long

    @Query(
        """
        UPDATE cached_answers
        SET syncState = 'SERVER_CONFIRMED', confirmedAtMillis = :confirmedAtMillis
        WHERE sessionId = :sessionId
          AND questionId = :questionId
          AND clientId = :clientId
          AND updatedAtMillis = :updatedAtMillis
          AND syncState != 'SERVER_CONFIRMED'
        """
    )
    suspend fun markConfirmed(
        sessionId: String,
        questionId: String,
        clientId: String,
        updatedAtMillis: Long,
        confirmedAtMillis: Long
    ): Int

    @Query(
        """
        UPDATE cached_answers
        SET syncState = 'SYNCING'
        WHERE sessionId = :sessionId
          AND questionId = :questionId
          AND clientId = :clientId
          AND syncState != 'SERVER_CONFIRMED'
        """
    )
    suspend fun markSyncing(sessionId: String, questionId: String, clientId: String): Int

    @Query(
        """
        UPDATE cached_answers
        SET syncState = 'LOCAL_PENDING'
        WHERE sessionId = :sessionId
          AND questionId = :questionId
          AND clientId = :clientId
          AND syncState = 'SYNCING'
        """
    )
    suspend fun releaseSyncing(sessionId: String, questionId: String, clientId: String): Int

    @Query(
        """
        UPDATE cached_answers
        SET clientId = :nextClientId
        WHERE sessionId = :sessionId
          AND questionId = :questionId
          AND clientId = :clientId
        """
    )
    suspend fun replaceClientId(
        sessionId: String,
        questionId: String,
        clientId: String,
        nextClientId: String
    ): Int
}
