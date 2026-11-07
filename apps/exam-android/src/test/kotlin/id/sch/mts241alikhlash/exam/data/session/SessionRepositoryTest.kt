package id.sch.mts241alikhlash.exam.data.session

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRepositoryTest {
    @Test
    fun `start then current returns id`() =
        runTest {
            val repo = SessionRepository(InMemorySessionStore())
            repo.start("sess-1")
            assertEquals("sess-1", repo.current())
        }

    @Test
    fun `finish clears session`() =
        runTest {
            val repo = SessionRepository(InMemorySessionStore())
            repo.start("sess-1")
            repo.finish()
            assertNull(repo.current())
            assertNull(repo.deadline())
        }

    @Test
    fun `blank id is rejected`() =
        runTest {
            val repo = SessionRepository(InMemorySessionStore())
            val error = runCatching { repo.start("  ") }.exceptionOrNull()
            assertTrue(error is IllegalArgumentException)
        }

    @Test
    fun `start trims surrounding whitespace`() =
        runTest {
            val repo = SessionRepository(InMemorySessionStore())
            repo.start("   sess-9   ")
            assertEquals("sess-9", repo.current())
        }

    @Test
    fun `start persists a deadline 90 minutes out`() =
        runTest {
            val now = 1_700_000_000_000L
            val store = InMemorySessionStore()
            val repo = SessionRepository(store, now = { now })

            repo.start("sess-1")

            assertEquals(now + 90L * 60L * 1000L, repo.deadline())
        }

    @Test
    fun `restarting the same session keeps the original deadline`() =
        runTest {
            var clock = 1_700_000_000_000L
            val repo = SessionRepository(InMemorySessionStore(), now = { clock })

            repo.start("sess-1")
            val firstDeadline = repo.deadline()

            clock += 20L * 60L * 1000L
            repo.start("sess-1")

            assertEquals(firstDeadline, repo.deadline())
        }

    @Test
    fun `finishIfCurrent clears the matching session`() =
        runTest {
            val repo = SessionRepository(InMemorySessionStore())
            repo.start("sess-1")

            assertTrue(repo.finishIfCurrent("sess-1"))
            assertNull(repo.current())
        }

    @Test
    fun `finishIfCurrent leaves a different session alone`() =
        runTest {
            val repo = SessionRepository(InMemorySessionStore())
            repo.start("sess-2")

            assertFalse(repo.finishIfCurrent("sess-1"))
            assertEquals("sess-2", repo.current())
        }
}

private class InMemorySessionStore : SessionStore {
    private var value: String? = null
    private var deadline: Long? = null

    override suspend fun load(): String? = value

    override suspend fun loadDeadline(): Long? = deadline

    override suspend fun save(
        id: String,
        deadline: Long,
    ) {
        value = id
        this.deadline = deadline
    }

    override suspend fun clear() {
        value = null
        deadline = null
    }
}

class SessionRepositoryServerDeadlineTest {
    @Test
    fun `start with explicit deadline persists it`() =
        runTest {
            val repo = SessionRepository(InMemorySessionStore(), now = { 1_000L })

            repo.start("sess-1", deadline = 61_000L)

            assertEquals(61_000L, repo.deadline())
        }

    @Test
    fun `restart of same session keeps the original explicit deadline`() =
        runTest {
            val repo = SessionRepository(InMemorySessionStore(), now = { 1_000L })
            repo.start("sess-1", deadline = 61_000L)

            repo.start("sess-1", deadline = 99_000L)

            assertEquals(61_000L, repo.deadline())
        }
}
