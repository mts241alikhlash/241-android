package id.sch.mts241alikhlash.istiqra.data.session

import id.sch.mts241alikhlash.istiqra.StubExamApi
import id.sch.mts241alikhlash.istiqra.api.exam.models.MyExamDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.StartSessionDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.StartedSessionDto
import id.sch.mts241alikhlash.istiqra.data.remote.ApiException
import id.sch.mts241alikhlash.istiqra.data.repository.ExamCatalogRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class ExamStartTest {
    private class Api(
        val result: () -> StartedSessionDto,
    ) : StubExamApi() {
        override suspend fun listExams(): List<MyExamDto> = emptyList()

        override suspend fun startSession(
            examId: String,
            request: StartSessionDto,
        ): StartedSessionDto = result()
    }

    @Test
    fun startExamSession_persists_session_with_server_deadline() =
        runTest {
            val sessions = SessionRepository(MemorySessionStore())
            val catalog =
                ExamCatalogRepository(
                    Api { StartedSessionDto("s1", deadline = 65_000L, serverTime = 5_000L) },
                    { "device-1" },
                    clock = ServerClock { 0L },
                )

            val started = startExamSession(catalog, sessions, "e1", "ABC")

            assertEquals("s1", started.sessionId)
            assertEquals("s1", sessions.current())
            assertEquals(65_000L, sessions.deadline())
        }

    @Test
    fun startExamSession_pulls_the_server_answers_for_the_new_session() =
        runTest {
            val sessions = SessionRepository(MemorySessionStore())
            val catalog = ExamCatalogRepository(Api { StartedSessionDto("s1", 65_000L, 5_000L) }, { "device-1" }, clock = ServerClock { 0L })
            val pulled = mutableListOf<String>()

            startExamSession(catalog, sessions, "e1", "ABC") { pulled += it }

            assertEquals(listOf("s1"), pulled)
            assertEquals("s1", sessions.current())
        }

    @Test
    fun startExamSession_still_starts_when_the_pull_fails() =
        runTest {
            val sessions = SessionRepository(MemorySessionStore())
            val catalog = ExamCatalogRepository(Api { StartedSessionDto("s1", 65_000L, 5_000L) }, { "device-1" }, clock = ServerClock { 0L })

            val started = startExamSession(catalog, sessions, "e1", "ABC") { throw IOException("offline") }

            assertEquals("s1", started.sessionId)
            assertEquals("s1", sessions.current())
        }

    @Test
    fun startExamSession_persists_nothing_when_server_refuses() =
        runTest {
            val sessions = SessionRepository(MemorySessionStore())
            val catalog = ExamCatalogRepository(Api { throw IOException("offline") }, { "device-1" })

            val error = runCatching { startExamSession(catalog, sessions, "e1", "ABC") }.exceptionOrNull()

            assertEquals(0, (error as ApiException).statusCode)
            assertNull(sessions.current())
        }
}

private class MemorySessionStore : SessionStore {
    private var id: String? = null
    private var deadline: Long? = null

    override suspend fun load(): String? = id

    override suspend fun loadDeadline(): Long? = deadline

    override suspend fun save(
        id: String,
        deadline: Long,
    ) {
        this.id = id
        this.deadline = deadline
    }

    override suspend fun clear() {
        id = null
        deadline = null
    }
}
