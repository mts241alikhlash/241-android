package id.sch.mts241alikhlash.cbt.data.repository

import id.sch.mts241alikhlash.cbt.EmptyQuestionDao
import id.sch.mts241alikhlash.cbt.InMemoryAnswerDao
import id.sch.mts241alikhlash.cbt.batchAck
import id.sch.mts241alikhlash.cbt.paperOf
import id.sch.mts241alikhlash.cbt.pendingAnswer
import id.sch.mts241alikhlash.cbt.data.local.AnswerSyncState
import id.sch.mts241alikhlash.cbt.data.model.PaperQuestionDto
import id.sch.mts241alikhlash.cbt.data.model.SaveAnswersBatchRequest
import id.sch.mts241alikhlash.cbt.data.model.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.cbt.data.model.SavedAnswerDto
import id.sch.mts241alikhlash.cbt.data.model.SkippedAnswerDto
import id.sch.mts241alikhlash.cbt.data.model.SubmitRequest
import id.sch.mts241alikhlash.cbt.data.model.SubmittedSessionDto
import id.sch.mts241alikhlash.cbt.data.remote.ApiErrorCode
import id.sch.mts241alikhlash.cbt.data.remote.ApiException
import id.sch.mts241alikhlash.cbt.data.remote.ExamApiService
import id.sch.mts241alikhlash.cbt.data.remote.NETWORK_ERROR_MESSAGE
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExamRepositoryTest {

    private fun repo(
        api: ExamApiService,
        dao: InMemoryAnswerDao,
        now: () -> Long = { 5_000L }
    ) = ExamRepository(api, dao, EmptyQuestionDao(), now = now)

    @Test
    fun `46 pending answers sync in a single batch call`() = runTest {
        val ids = (1..46).map { "q$it" }
        val dao = InMemoryAnswerDao(*ids.map { pendingAnswer(it) }.toTypedArray())
        val api = BatchApi()

        val outcome = repo(api, dao).syncPendingBatch("s1")

        assertEquals(1, api.batches.size)
        assertEquals(46, api.batches.single().answers.size)
        assertEquals(46, outcome.confirmed)
        assertEquals(0, outcome.pending)
        assertTrue(dao.rows.values.all { it.syncState == AnswerSyncState.SERVER_CONFIRMED })
    }

    @Test
    fun `more than one hundred pending answers are chunked`() = runTest {
        val ids = (1..150).map { "q$it" }
        val dao = InMemoryAnswerDao(*ids.map { pendingAnswer(it) }.toTypedArray())
        val api = BatchApi()

        val outcome = repo(api, dao).syncPendingBatch("s1")

        assertEquals(listOf(100, 50), api.batches.map { it.answers.size })
        assertEquals(150, outcome.confirmed)
        assertEquals(0, outcome.pending)
    }

    @Test
    fun `an answer edited while its batch is in flight stays pending`() = runTest {
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        val api = BatchApi()
        val gate = CompletableDeferred<Unit>()
        api.gate = gate

        val job = launch { repo(api, dao).syncPendingBatch("s1") }
        runCurrent()

        dao.saveAnswer(
            pendingAnswer("q1", answer = """{"key":"B"}""", updatedAtMillis = 9_000L, clientId = "c-q1-v2")
        )
        gate.complete(Unit)
        job.join()

        val row = dao.rows.getValue("q1")
        assertEquals(AnswerSyncState.LOCAL_PENDING, row.syncState)
        assertEquals("""{"key":"B"}""", row.answer)
        assertEquals(1, dao.countPendingAnswers("s1"))
    }

    @Test
    fun `submit sends the remaining pending answers with the expected count`() = runTest {
        val ids = (1..46).map { "q$it" }
        val dao = InMemoryAnswerDao(*ids.map { pendingAnswer(it) }.toTypedArray())
        val api = BatchApi()

        repo(api, dao).submit("s1", violations = 2)

        val request = api.submits.single()
        assertEquals(46, request.expectedAnswerCount)
        assertEquals(46, request.pendingAnswers?.size)
        assertEquals(2, request.violations)
        assertEquals(emptyList<SaveAnswersBatchRequest>(), api.batches)
        assertTrue(dao.rows.values.all { it.syncState == AnswerSyncState.SERVER_CONFIRMED })
    }

    @Test
    fun `an expired submit does not confirm the answers it carried`() = runTest {
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        val api = BatchApi()
        api.expired = true

        val result = repo(api, dao).submit("s1")

        assertEquals("EXPIRED", result.status)
        assertEquals(false, result.answersStored)
        assertEquals(AnswerSyncState.LOCAL_PENDING, dao.rows.getValue("q1").syncState)
        assertEquals(1, dao.countPendingAnswers("s1"))
    }

    @Test
    fun `a closed batch window is reported with its error code`() = runTest {
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        val api = BatchApi()
        api.batchError = ApiException(
            409,
            "Waktu pengiriman jawaban sudah habis",
            ApiErrorCode.WINDOW_CLOSED
        )

        val outcome = repo(api, dao).syncPendingBatch("s1")

        assertEquals(ApiErrorCode.WINDOW_CLOSED, outcome.closedCode)
        assertEquals("Waktu pengiriman jawaban sudah habis", outcome.closedMessage)
    }

    @Test
    fun `the server time from a batch response reaches the clock`() = runTest {
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        val api = BatchApi()
        val times = mutableListOf<String>()
        val repository = ExamRepository(
            api,
            dao,
            EmptyQuestionDao(),
            now = { 5_000L },
            onServerTime = { times.add(it) }
        )

        repository.syncPendingBatch("s1")

        assertEquals(listOf("2026-10-27T00:00:00Z"), times)
    }

    @Test
    fun `a skipped answer is dropped and re-sent with a fresh client id`() = runTest {
        val dao = InMemoryAnswerDao(pendingAnswer("q1", clientId = "stale-client"))
        val api = BatchApi(refuseClientIds = mutableSetOf("stale-client"))

        val outcome = repo(api, dao).syncPendingBatch("s1")

        assertEquals(1, outcome.skipped)
        assertEquals(0, outcome.pending)
        assertEquals(2, api.batches.size)
        val first = api.batches[0].answers.single().clientId
        val second = api.batches[1].answers.single().clientId
        assertEquals("stale-client", first)
        assertNotEquals("a skipped item must not be retried with the same client id", first, second)
        assertEquals(AnswerSyncState.SERVER_CONFIRMED, dao.rows.getValue("q1").syncState)
    }

    @Test
    fun `a batch failure larger than one chunk fails loudly instead of looping`() = runTest {
        val ids = (1..260).map { "q$it" }
        val dao = InMemoryAnswerDao(*ids.map { pendingAnswer(it) }.toTypedArray())
        val api = BatchApi(offline = true)

        val error = runCatching { repo(api, dao).submit("s1") }.exceptionOrNull()

        assertTrue("expected an ApiException, got $error", error is ApiException)
        assertEquals(NETWORK_ERROR_MESSAGE, error?.message)
        assertEquals(1, api.batches.size)
        assertEquals(260, dao.countPendingAnswers("s1"))
        assertTrue(dao.rows.values.none { it.syncState == AnswerSyncState.SYNCING })
    }

    @Test
    fun `a chunk that confirms nothing stops after one attempt`() = runTest {
        val ids = (1..260).map { "q$it" }
        val dao = InMemoryAnswerDao(*ids.map { pendingAnswer(it) }.toTypedArray())
        val api = BatchApi(confirmNothing = true)

        val error = runCatching { repo(api, dao).submit("s1") }.exceptionOrNull()

        assertTrue("expected an ApiException, got $error", error is ApiException)
        assertTrue(error?.message?.contains("belum terkirim") == true)
        assertEquals(1, api.batches.size)
    }

    @Test
    fun `rows are SYNCING while the batch is in flight and pending again after a failure`() = runTest {
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        val api = BatchApi(offline = true)
        val gate = CompletableDeferred<Unit>()
        api.gate = gate

        val job = launch { repo(api, dao).syncPendingBatch("s1") }
        runCurrent()

        assertEquals(AnswerSyncState.SYNCING, dao.rows.getValue("q1").syncState)

        gate.complete(Unit)
        job.join()

        assertEquals(AnswerSyncState.LOCAL_PENDING, dao.rows.getValue("q1").syncState)
    }

    @Test
    fun `a 409 with a session-lost code is not retried`() = runTest {
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        val api = BatchApi(
            submitError = ApiException(409, "Sesi dilepas. Mulai ujian lagi", ApiErrorCode.SESSION_LOST)
        )

        val error = runCatching { repo(api, dao).submit("s1") }.exceptionOrNull()

        assertEquals(ApiErrorCode.SESSION_LOST, (error as ApiException).code)
        assertEquals(1, api.submits.size)
    }

    @Test
    fun `an unknown code is not treated as a lost session`() {
        assertTrue(!isSessionLost(ApiException(404, "Jadwal ujian tidak ditemukan", "SCHEDULE_GONE")))
        assertTrue(isSessionLost(ApiException(409, "Sesi dilepas. Mulai ujian lagi", ApiErrorCode.SESSION_LOST)))
        assertTrue("a message-only body still classifies", isSessionLost(ApiException(409, "Sesi dilepas. Mulai ujian lagi")))
    }

    @Test
    fun `a shortfall code is retried once after a batch sync`() = runTest {
        val dao = InMemoryAnswerDao(pendingAnswer("q1"))
        val api = BatchApi(
            submitError = ApiException(409, "Masih ada jawaban yang belum diterima server (0/1), silakan coba lagi", ApiErrorCode.SHORTFALL),
            failSubmitOnce = true
        )

        val result = repo(api, dao).submit("s1")

        assertEquals("SUBMITTED", result.status)
        assertEquals(2, api.submits.size)
        assertEquals(AnswerSyncState.SERVER_CONFIRMED, dao.rows.getValue("q1").syncState)
    }

    @Test
    fun `editing a confirmed answer mints a new client id and goes back to pending`() = runTest {
        val dao = InMemoryAnswerDao(
            pendingAnswer("q1").copy(
                syncState = AnswerSyncState.SERVER_CONFIRMED,
                confirmedAtMillis = 4_000L
            )
        )
        var clock = 8_000L
        val repository = repo(BatchApi(), dao, now = { clock })

        clock = 9_000L
        val entity = repository.recordAnswer("q1", "s1", """{"key":"C"}""")

        assertEquals(AnswerSyncState.LOCAL_PENDING, entity.syncState)
        assertNotEquals("c-q1", entity.clientId)
        assertEquals(null, entity.confirmedAtMillis)
    }

    @Test
    fun `the busy flag stays set until the outermost submit returns`() = runTest {
        val ids = (1..251).map { "q$it" }
        val dao = InMemoryAnswerDao(*ids.map { pendingAnswer(it) }.toTypedArray())
        val api = BatchApi()
        val batchGate = CompletableDeferred<Unit>()
        val submitGate = CompletableDeferred<Unit>()
        api.gate = batchGate
        api.submitGate = submitGate
        val repository = repo(api, dao)

        val job = launch { repository.submit("s1") }
        runCurrent()
        assertTrue("the nested sync is still running", repository.isSyncingOrSubmitting)

        batchGate.complete(Unit)
        runCurrent()
        assertEquals(251, api.batches.sumOf { it.answers.size })
        assertEquals(1, api.submits.size)
        assertTrue("the outer submit is still running", repository.isSyncingOrSubmitting)

        submitGate.complete(Unit)
        job.join()

        assertTrue(!repository.isSyncingOrSubmitting)
    }
}

private class BatchApi(
    private val refuseClientIds: MutableSet<String> = mutableSetOf(),
    private val offline: Boolean = false,
    private val confirmNothing: Boolean = false,
    private var submitError: ApiException? = null,
    private var failSubmitOnce: Boolean = false
) : ExamApiService {
    val batches = mutableListOf<SaveAnswersBatchRequest>()
    val submits = mutableListOf<SubmitRequest>()
    var gate: CompletableDeferred<Unit>? = null
    var submitGate: CompletableDeferred<Unit>? = null
    var batchError: ApiException? = null
    var expired: Boolean = false

    override suspend fun getQuestions(sessionId: String) = paperOf()

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchRequest
    ): SaveAnswersBatchResultDto {
        batches += request
        gate?.await()
        batchError?.let { throw it }
        if (offline) throw java.io.IOException("offline")
        if (confirmNothing) {
            return SaveAnswersBatchResultDto(
                confirmed = emptyList(),
                skipped = emptyList(),
                confirmedCount = 0,
                serverTime = "2026-10-27T00:00:00Z"
            )
        }
        val skipped = request.answers
            .filter { it.clientId in refuseClientIds }
            .map { SkippedAnswerDto(it.questionId, it.clientId) }
        skipped.forEach { refuseClientIds.remove(it.clientId) }
        return batchAck(request, skipped)
    }

    override suspend fun submit(sessionId: String, request: SubmitRequest): SubmittedSessionDto {
        submits += request
        submitGate?.await()
        val failure = submitError
        if (failure != null && (failSubmitOnce || submits.size == 1)) {
            if (failSubmitOnce) submitError = null
            throw failure
        }
        return SubmittedSessionDto(
            status = if (expired) "EXPIRED" else "SUBMITTED",
            submittedAt = "2026-10-27T00:00:00Z",
            confirmedAnswerCount = request.pendingAnswers?.size ?: 0,
            answersStored = !expired,
            serverTime = "2026-10-27T00:00:00Z"
        )
    }
}
