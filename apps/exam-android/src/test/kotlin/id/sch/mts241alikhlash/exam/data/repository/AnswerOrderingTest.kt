package id.sch.mts241alikhlash.exam.data.repository

import id.sch.mts241alikhlash.exam.EmptyQuestionDao
import id.sch.mts241alikhlash.exam.InMemoryAnswerDao
import id.sch.mts241alikhlash.exam.batchAck
import id.sch.mts241alikhlash.exam.data.local.AnswerSyncState
import id.sch.mts241alikhlash.exam.data.model.AnswerPayload
import id.sch.mts241alikhlash.exam.data.model.RecordSessionEventsRequest
import id.sch.mts241alikhlash.exam.data.model.RecordSessionEventsResponse
import id.sch.mts241alikhlash.exam.data.model.ResumeSessionRequest
import id.sch.mts241alikhlash.exam.data.model.ResumedSessionDto
import id.sch.mts241alikhlash.exam.data.model.SaveAnswersBatchRequest
import id.sch.mts241alikhlash.exam.data.model.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.exam.data.model.SavedAnswerDto
import id.sch.mts241alikhlash.exam.data.model.SubmitRequest
import id.sch.mts241alikhlash.exam.data.model.SubmittedSessionDto
import id.sch.mts241alikhlash.exam.data.model.ViolationCountDto
import id.sch.mts241alikhlash.exam.data.model.ViolationReport
import id.sch.mts241alikhlash.exam.data.remote.ExamApiService
import id.sch.mts241alikhlash.exam.paperOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AnswerOrderingTest {
    private fun http(
        code: Int,
        message: String,
    ) = HttpException(
        Response.error<Any>(
            code,
            """{"statusCode":$code,"message":"$message","data":null}""".toResponseBody("application/json".toMediaType()),
        ),
    )

    private fun repo(
        api: ExamApiService,
        dao: InMemoryAnswerDao,
        now: () -> Long = { 1_000L },
    ) = ExamRepository(api, dao, EmptyQuestionDao(), now = now)

    @Test
    fun `a newer edit of the same answer is the one that lands`() =
        runTest {
            var clock = 1_000L
            val dao = InMemoryAnswerDao()
            val repository = repo(OrderApi(), dao, now = { clock })

            repository.recordAnswer("q1", "s1", AnswerPayload.Choice("A").encode())
            clock = 2_000L
            repository.recordAnswer("q1", "s1", AnswerPayload.Choice("B").encode())

            assertEquals(AnswerPayload.Choice("B").encode(), dao.rows.getValue("q1").answer)
            assertEquals(2_000L, dao.rows.getValue("q1").updatedAtMillis)
            assertEquals(AnswerSyncState.LOCAL_PENDING, dao.rows.getValue("q1").syncState)
        }

    @Test
    fun `editing a confirmed answer mints a fresh client id`() =
        runTest {
            var clock = 1_000L
            val dao = InMemoryAnswerDao()
            val repository = repo(OrderApi(), dao, now = { clock })

            val first = repository.recordAnswer("q1", "s1", AnswerPayload.Choice("A").encode())
            dao.saveAnswer(first.copy(syncState = AnswerSyncState.SERVER_CONFIRMED, confirmedAtMillis = 1_000L))
            clock = 2_000L
            val second = repository.recordAnswer("q1", "s1", AnswerPayload.Choice("C").encode())

            assertNotEquals(first.clientId, second.clientId)
            assertEquals(AnswerSyncState.LOCAL_PENDING, second.syncState)
            assertEquals(null, second.confirmedAtMillis)
        }

    @Test
    fun `a closed window stops the batch and keeps every row pending`() =
        runTest {
            val dao =
                InMemoryAnswerDao(
                    id.sch.mts241alikhlash.exam
                        .pendingAnswer("q1"),
                    id.sch.mts241alikhlash.exam
                        .pendingAnswer("q2"),
                )
            val api = OrderApi(error = http(409, "Waktu pengiriman jawaban sudah habis"))

            val outcome = repo(api, dao).syncPendingBatch("s1")

            assertEquals("Waktu pengiriman jawaban sudah habis", outcome.closedMessage)
            assertEquals(1, api.batches.size)
            assertEquals(2, outcome.pending)
            assertTrue(dao.rows.values.all { it.syncState == AnswerSyncState.LOCAL_PENDING })
        }

    @Test
    fun `a rejected batch reports the server message once`() =
        runTest {
            val dao =
                InMemoryAnswerDao(
                    id.sch.mts241alikhlash.exam
                        .pendingAnswer("q1"),
                )
            val api = OrderApi(error = http(400, "Bentuk jawaban tidak sesuai dengan jenis soal"))

            val outcome = repo(api, dao).syncPendingBatch("s1")

            assertEquals(1, outcome.rejected)
            assertEquals("Bentuk jawaban tidak sesuai dengan jenis soal", outcome.rejectedMessage)
            assertEquals(null, outcome.closedMessage)
        }

    @Test
    fun `a network failure marks the pass offline and stops it`() =
        runTest {
            val dao =
                InMemoryAnswerDao(
                    id.sch.mts241alikhlash.exam
                        .pendingAnswer("q1"),
                    id.sch.mts241alikhlash.exam
                        .pendingAnswer("q2"),
                )
            val api = OrderApi(error = IOException("offline"))

            val outcome = repo(api, dao).syncPendingBatch("s1")

            assertTrue(outcome.offline)
            assertEquals(1, api.batches.size)
            assertEquals(2, outcome.pending)
        }

    @Test
    fun `pulls server answers without overwriting unsent local ones`() =
        runTest {
            val dao =
                InMemoryAnswerDao(
                    id.sch.mts241alikhlash.exam.pendingAnswer(
                        "q1",
                        answer = AnswerPayload.Choice("A").encode(),
                    ),
                )
            val api =
                OrderApi(
                    remote =
                        listOf(
                            SavedAnswerDto("q1", AnswerPayload.Choice("B").toJson()),
                            SavedAnswerDto("q2", AnswerPayload.Multiple(listOf("C", "A")).toJson()),
                            SavedAnswerDto("q3", kotlinx.serialization.json.JsonObject(emptyMap())),
                        ),
                )

            val imported = repo(api, dao, now = { 9_000L }).pullServerAnswers("s1")

            assertEquals(1, imported)
            assertEquals(AnswerPayload.Choice("A").encode(), dao.rows.getValue("q1").answer)
            assertEquals(AnswerSyncState.LOCAL_PENDING, dao.rows.getValue("q1").syncState)
            val pulled = dao.rows.getValue("q2")
            assertEquals(AnswerPayload.Multiple(listOf("A", "C")).encode(), pulled.answer)
            assertEquals(AnswerSyncState.SERVER_CONFIRMED, pulled.syncState)
            assertEquals(null, dao.rows["q3"])
        }

    @Test
    fun `the same client id is never produced for another question`() {
        val a = answerClientId("s1", "q1", """{"key":"A"}""", 1_000L)
        val b = answerClientId("s1", "q2", """{"key":"A"}""", 1_000L)
        assertNotEquals(a, b)
    }

    @Test
    fun `a session-lost message is recognised`() {
        assertTrue(isSessionLostMessage("Sesi dilepas. Mulai ujian lagi"))
        assertTrue(isSessionLostMessage("Sesi aktif di perangkat lain. Minta pengawas melepas sesi"))
        assertTrue(!isSessionLostMessage("Waktu pengiriman jawaban sudah habis"))
    }
}

private class OrderApi(
    private val error: Exception? = null,
    private val remote: List<SavedAnswerDto> = emptyList(),
) : ExamApiService {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.exam.data.model.SessionHeartbeatResponse =
        id.sch.mts241alikhlash.exam.data.model
            .SessionHeartbeatResponse()

    val batches = mutableListOf<SaveAnswersBatchRequest>()

    override suspend fun getQuestions(sessionId: String) = paperOf()

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = remote

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchRequest,
    ): SaveAnswersBatchResultDto {
        batches += request
        error?.let { throw it }
        return batchAck(request)
    }

    override suspend fun submit(
        sessionId: String,
        request: SubmitRequest,
    ): SubmittedSessionDto = SubmittedSessionDto("SUBMITTED")

    override suspend fun recordSessionEvents(
        sessionId: String,
        request: RecordSessionEventsRequest,
    ): RecordSessionEventsResponse = RecordSessionEventsResponse()

    override suspend fun resumeSession(
        sessionId: String,
        request: ResumeSessionRequest,
    ): ResumedSessionDto =
        ResumedSessionDto(
            sessionId = sessionId,
            status = "IN_PROGRESS",
            deadline = "",
            serverTime = "",
        )

    override suspend fun reportViolations(
        sessionId: String,
        request: ViolationReport,
    ): ViolationCountDto = ViolationCountDto(request.count)

    override suspend fun reportSuspension(
        sessionId: String,
        request: id.sch.mts241alikhlash.exam.data.model.SuspensionRequest,
    ): id.sch.mts241alikhlash.exam.data.model.SuspensionResponseDto = throw UnsupportedOperationException("not used")
}
