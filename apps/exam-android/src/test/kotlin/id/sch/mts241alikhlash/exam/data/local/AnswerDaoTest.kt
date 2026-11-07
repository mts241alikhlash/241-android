package id.sch.mts241alikhlash.exam.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnswerDaoTest {
    private lateinit var db: ExamDatabase
    private lateinit var dao: AnswerDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, ExamDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = db.answerDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `markConfirmed marks the matching revision`() =
        runTest {
            dao.saveAnswer(answer("q1", "A", updatedAtMillis = 100L))

            val updated = dao.markConfirmed("s1", "q1", "c-q1", 100L, 200L)

            assertEquals(1, updated)
            val row = dao.getAnswer("s1", "q1")!!
            assertEquals(AnswerSyncState.SERVER_CONFIRMED, row.syncState)
            assertEquals(200L, row.confirmedAtMillis)
            assertEquals(0, dao.countPendingAnswers("s1"))
        }

    @Test
    fun `markConfirmed refuses a stale revision`() =
        runTest {
            dao.saveAnswer(answer("q1", "B", updatedAtMillis = 200L))

            val updated = dao.markConfirmed("s1", "q1", "c-q1", 100L, 300L)

            assertEquals("stale revision must not be marked", 0, updated)
            assertEquals(AnswerSyncState.LOCAL_PENDING, dao.getAnswer("s1", "q1")!!.syncState)
            assertEquals(1, dao.countPendingAnswers("s1"))
        }

    @Test
    fun `markConfirmed refuses a replaced client id`() =
        runTest {
            dao.saveAnswer(answer("q1", "A", updatedAtMillis = 100L, clientId = "new"))

            val updated = dao.markConfirmed("s1", "q1", "c-q1", 100L, 200L)

            assertEquals(0, updated)
            assertEquals(AnswerSyncState.LOCAL_PENDING, dao.getAnswer("s1", "q1")!!.syncState)
        }

    @Test
    fun `markConfirmed refuses to touch another session`() =
        runTest {
            dao.saveAnswer(answer("q1", "A", updatedAtMillis = 100L, sessionId = "s2"))

            val updated = dao.markConfirmed("s1", "q1", "c-q1", 100L, 200L)

            assertEquals(0, updated)
            assertEquals(AnswerSyncState.LOCAL_PENDING, dao.getAnswer("s2", "q1")!!.syncState)
        }

    @Test
    fun `pending queries are scoped to the session and skip confirmed rows`() =
        runTest {
            dao.saveAnswer(answer("q1", "A", updatedAtMillis = 100L, sessionId = "s1"))
            dao.saveAnswer(answer("q2", "A", updatedAtMillis = 100L, sessionId = "s2"))
            dao.saveAnswer(
                answer("q3", "A", updatedAtMillis = 100L, sessionId = "s1")
                    .copy(syncState = AnswerSyncState.SERVER_CONFIRMED),
            )

            assertEquals(1, dao.countPendingAnswers("s1"))
            assertEquals(listOf("q1"), dao.getPendingAnswers("s1").map { it.questionId })
        }

    @Test
    fun `replaceClientId swaps only the refused client id`() =
        runTest {
            dao.saveAnswer(answer("q1", "A", updatedAtMillis = 100L, clientId = "old"))

            val updated = dao.replaceClientId("s1", "q1", "old", "fresh")

            assertEquals(1, updated)
            val row = dao.getAnswer("s1", "q1")!!
            assertEquals("fresh", row.clientId)
            assertEquals(AnswerSyncState.LOCAL_PENDING, row.syncState)
        }

    @Test
    fun `replaceClientId ignores a mismatched client id`() =
        runTest {
            dao.saveAnswer(answer("q1", "A", updatedAtMillis = 100L, clientId = "current"))

            assertEquals(0, dao.replaceClientId("s1", "q1", "old", "fresh"))
            assertEquals("current", dao.getAnswer("s1", "q1")!!.clientId)
        }

    @Test
    fun `a blank answer is never counted as answered`() =
        runTest {
            dao.saveAnswer(answer("q1", "A", updatedAtMillis = 100L))
            dao.saveAnswer(answer("q2", "", updatedAtMillis = 100L))

            assertEquals(1, dao.countAnswered("s1"))
        }

    private fun answer(
        questionId: String,
        answer: String,
        updatedAtMillis: Long,
        sessionId: String = "s1",
        clientId: String = "c-$questionId",
    ) = CachedAnswerEntity(
        questionId = questionId,
        sessionId = sessionId,
        answer = answer,
        isDoubtful = false,
        clientId = clientId,
        syncState = AnswerSyncState.LOCAL_PENDING,
        updatedAtMillis = updatedAtMillis,
    )
}
