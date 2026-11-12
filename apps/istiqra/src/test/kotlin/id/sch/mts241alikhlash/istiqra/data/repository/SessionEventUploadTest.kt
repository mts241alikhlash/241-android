package id.sch.mts241alikhlash.istiqra.data.repository

import id.sch.mts241alikhlash.istiqra.EmptyQuestionDao
import id.sch.mts241alikhlash.istiqra.InMemoryAnswerDao
import id.sch.mts241alikhlash.istiqra.StubExamApi
import id.sch.mts241alikhlash.istiqra.api.exam.apis.StudentExamsApi
import id.sch.mts241alikhlash.istiqra.api.exam.models.RecordSessionEventsRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.RecordSessionEventsResponseDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.RecordViolationsRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.ResumeSessionRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.ResumeSessionResponseDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswersBatchDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SavedAnswerDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SubmitSessionRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SubmitSessionResponseDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.ViolationCountDto
import id.sch.mts241alikhlash.istiqra.data.local.CachedSessionEventEntity
import id.sch.mts241alikhlash.istiqra.data.local.SessionEventDao
import id.sch.mts241alikhlash.istiqra.data.remote.ApiException
import id.sch.mts241alikhlash.istiqra.eventsResponse
import id.sch.mts241alikhlash.istiqra.heartbeatOf
import id.sch.mts241alikhlash.istiqra.paperOf
import id.sch.mts241alikhlash.istiqra.submitted
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SessionEventUploadTest {
    private fun repo(
        api: StudentExamsApi,
        events: InMemorySessionEventDao,
    ) = ExamRepository(
        api = api,
        dao = InMemoryAnswerDao(),
        questionDao = EmptyQuestionDao(),
        eventDao = events,
        now = { 1_000L },
    )

    @Test
    fun `a queued event is uploaded once and marked synced`() =
        runTest {
            val events = InMemorySessionEventDao(leftApp("c1"))
            val api = EventsApi()

            val uploaded = repo(api, events).uploadSessionEvents("s1").uploaded

            assertEquals(1, uploaded)
            assertEquals(1, api.requests.size)
            assertEquals(
                listOf("c1"),
                api.requests
                    .single()
                    .events
                    .map { it.clientId },
            )
            assertEquals(
                "2026-11-01T01:00:00.000Z",
                api.requests
                    .single()
                    .events
                    .single()
                    .occurredAt,
            )
            assertTrue(events.rows.getValue("c1").isSynced)
        }

    @Test
    fun `uploading twice does not send the same event again`() =
        runTest {
            val events = InMemorySessionEventDao(leftApp("c1"))
            val api = EventsApi()
            val repository = repo(api, events)

            repository.uploadSessionEvents("s1")
            repository.uploadSessionEvents("s1")

            assertEquals(1, api.requests.size)
        }

    @Test
    fun `a failed upload leaves the event unsynced`() =
        runTest {
            val events = InMemorySessionEventDao(leftApp("c1"))
            val api = EventsApi(offline = true)

            val failure = runCatching { repo(api, events).uploadSessionEvents("s1") }

            assertTrue(failure.isFailure)
            assertEquals(false, events.rows.getValue("c1").isSynced)
        }

    @Test
    fun `an empty queue does not call the server`() =
        runTest {
            val api = EventsApi()

            val uploaded = repo(api, InMemorySessionEventDao()).uploadSessionEvents("s1").uploaded

            assertEquals(0, uploaded)
            assertTrue(api.requests.isEmpty())
        }

    @Test
    fun `more than two hundred events are chunked`() =
        runTest {
            val events =
                InMemorySessionEventDao(
                    *((1..450).map { leftApp("c$it") }.toTypedArray()),
                )
            val api = EventsApi()

            val uploaded = repo(api, events).uploadSessionEvents("s1").uploaded

            assertEquals(450, uploaded)
            assertEquals(listOf(200, 200, 50), api.requests.map { it.events.size })
            assertTrue(events.rows.values.all { it.isSynced })
        }

    @Test
    fun `an upload that crosses the threshold reports the lock`() =
        runTest {
            val events = InMemorySessionEventDao(leftApp("c1"))
            val api = EventsApi(lockedByViolation = true)

            val report = repo(api, events).uploadSessionEvents("s1")

            assertEquals(true, report.lockedByViolation)
            assertEquals(1, report.uploaded)
        }

    @Test
    fun `an upload that demands a new token reports it`() =
        runTest {
            val events = InMemorySessionEventDao(leftApp("c1"))
            val api = EventsApi(requiresTokenReentry = true)

            val report = repo(api, events).uploadSessionEvents("s1")

            assertEquals(true, report.requiresTokenReentry)
            assertEquals(false, report.lockedByViolation)
        }

    @Test
    fun `resume sends the token and the device and reports the deadline`() =
        runTest {
            val api = EventsApi()
            val repository =
                ExamRepository(
                    api = api,
                    dao = InMemoryAnswerDao(),
                    questionDao = EmptyQuestionDao(),
                    eventDao = InMemorySessionEventDao(),
                    deviceId = { "tablet-1" },
                    now = { 1_000L },
                )

            val resumed = repository.resumeSession("s1", " ABC123 ")

            assertEquals("IN_PROGRESS", resumed.status)
            assertEquals(1_793_496_600_000L, resumed.deadlineMillis)
            assertEquals(false, resumed.requiresTokenReentry)
            assertEquals(ResumeSessionRequestDto("ABC123", "tablet-1"), api.resume)
        }

    @Test
    fun `a locked session keeps the server error code`() =
        runTest {
            val api = EventsApi(resumeError = ApiException(403, "Sesi terkunci", "SESSION_LOCKED_BY_VIOLATION"))
            val repository =
                ExamRepository(
                    api = api,
                    dao = InMemoryAnswerDao(),
                    questionDao = EmptyQuestionDao(),
                    eventDao = InMemorySessionEventDao(),
                )

            val failure = runCatching { repository.resumeSession("s1", "ABC123") }

            assertEquals(
                "SESSION_LOCKED_BY_VIOLATION",
                (failure.exceptionOrNull() as ApiException).code,
            )
        }
}

private fun leftApp(clientId: String) =
    CachedSessionEventEntity(
        clientId = clientId,
        sessionId = "s1",
        type = "LEFT_APP",
        isViolation = true,
        occurredAtMs = 1_793_494_800_000L,
        detail = "Siswa keluar dari aplikasi ujian",
    )

private class InMemorySessionEventDao(
    vararg initial: CachedSessionEventEntity,
) : SessionEventDao {
    val rows = initial.associateBy { it.clientId }.toMutableMap()

    override suspend fun insert(event: CachedSessionEventEntity) {
        rows.putIfAbsent(event.clientId, event)
    }

    override suspend fun unsynced(sessionId: String) =
        rows.values
            .filter { it.sessionId == sessionId && !it.isSynced }
            .sortedBy { it.occurredAtMs }

    override suspend fun markSynced(clientIds: List<String>) {
        for (id in clientIds) {
            rows[id]?.let { rows[id] = it.copy(isSynced = true) }
        }
    }

    override suspend fun prune(sessionId: String) = Unit
}

private class EventsApi(
    private val offline: Boolean = false,
    private val resumeError: ApiException? = null,
    private val lockedByViolation: Boolean = false,
    private val requiresTokenReentry: Boolean = false,
) : StubExamApi() {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.istiqra.api.exam.models.SessionHeartbeatDto = heartbeatOf()

    val requests = mutableListOf<RecordSessionEventsRequestDto>()
    var resume: ResumeSessionRequestDto? = null

    override suspend fun getQuestions(sessionId: String): id.sch.mts241alikhlash.istiqra.api.exam.models.SessionPaperDto = paperOf()

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchDto,
    ): SaveAnswersBatchResultDto = throw UnsupportedOperationException()

    override suspend fun submit(
        sessionId: String,
        request: SubmitSessionRequestDto,
    ): SubmitSessionResponseDto = submitted(SubmitSessionResponseDto.Status.SUBMITTED)

    override suspend fun reportViolations(
        sessionId: String,
        request: RecordViolationsRequestDto,
    ): ViolationCountDto = ViolationCountDto(request.count)

    override suspend fun reportSuspension(
        sessionId: String,
        request: id.sch.mts241alikhlash.istiqra.api.exam.models.SessionSuspensionRequestDto,
    ): id.sch.mts241alikhlash.istiqra.api.exam.models.SessionSuspensionDto = throw UnsupportedOperationException("not used")

    override suspend fun recordSessionEvents(
        sessionId: String,
        request: RecordSessionEventsRequestDto,
    ): RecordSessionEventsResponseDto {
        if (offline) throw IOException("offline")
        requests += request
        return eventsResponse(
            violationCount = request.events.count { it.isViolation },
            requiresTokenReentry = requiresTokenReentry,
            lockedByViolation = lockedByViolation,
            serverTime = "2026-11-01T01:00:00.000Z",
        )
    }

    override suspend fun resumeSession(
        sessionId: String,
        request: ResumeSessionRequestDto,
    ): ResumeSessionResponseDto {
        resumeError?.let { throw it }
        resume = request
        return ResumeSessionResponseDto(
            sessionId = sessionId,
            status = ResumeSessionResponseDto.Status.IN_PROGRESS,
            deadline = "2026-11-01T01:30:00.000Z",
            serverTime = "2026-11-01T01:00:00.000Z",
            requiresTokenReentry = false,
        )
    }
}
