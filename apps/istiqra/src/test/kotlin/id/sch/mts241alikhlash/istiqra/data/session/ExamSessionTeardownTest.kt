package id.sch.mts241alikhlash.istiqra.data.session

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamSessionTeardownTest {
    @Test
    fun `session is cleared even when cache purge fails`() =
        runTest {
            val sessionRepository = SessionRepository(RecordingSessionStore().apply { save("s1", 1L) })

            endExamSession(sessionRepository, "s1") {
                throw IllegalStateException("database closed")
            }

            assertNull(sessionRepository.current())
        }

    @Test
    fun `cache purge receives the finished session id`() =
        runTest {
            val sessionRepository = SessionRepository(RecordingSessionStore().apply { save("s1", 1L) })
            val purged = mutableListOf<String>()

            endExamSession(sessionRepository, "s1") { purged.add(it) }

            assertEquals(listOf("s1"), purged)
            assertNull(sessionRepository.current())
        }

    @Test
    fun `missing session id skips the purge`() =
        runTest {
            val sessionRepository = SessionRepository(RecordingSessionStore())
            var purgeCalled = false

            endExamSession(sessionRepository, null) { purgeCalled = true }

            assertEquals(false, purgeCalled)
            assertNull(sessionRepository.current())
        }

    @Test
    fun `session is cleared before the cache purge runs`() =
        runTest {
            val store = RecordingSessionStore().apply { save("s1", 1L) }
            val sessionRepository = SessionRepository(store)
            var sessionIdDuringPurge: String? = "<not called>"

            endExamSession(sessionRepository, "s1") {
                sessionIdDuringPurge = store.load()
            }

            assertNull("session must already be cleared when purge starts", sessionIdDuringPurge)
        }

    @Test
    fun `stale teardown does not clear a newly started session`() =
        runTest {
            val sessionRepository = SessionRepository(RecordingSessionStore().apply { save("s2", 1L) })
            var purged = false

            val ended = endExamSession(sessionRepository, "s1") { purged = true }

            assertFalse("old teardown must not report success", ended)
            assertFalse("old teardown must not purge the new session", purged)
            assertEquals("s2", sessionRepository.current())
        }

    @Test
    fun `session already cleared by the submit still purges the cache and succeeds`() =
        runTest {
            val sessionRepository = SessionRepository(RecordingSessionStore())
            val purged = mutableListOf<String>()

            val ended = endExamSession(sessionRepository, "s1") { purged.add(it) }

            assertTrue("a submit that cleared the session is a clean end", ended)
            assertEquals(listOf("s1"), purged)
        }
}

private class RecordingSessionStore : SessionStore {
    private var value: String? = null
    private var deadline: Long? = null
    val events = mutableListOf<String>()

    override suspend fun load(): String? = value

    override suspend fun loadDeadline(): Long? = deadline

    override suspend fun save(
        id: String,
        deadline: Long,
    ) {
        events.add("save")
        value = id
        this.deadline = deadline
    }

    override suspend fun clear() {
        events.add("clear")
        value = null
        deadline = null
    }
}
