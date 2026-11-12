package id.sch.mts241alikhlash.istiqra.data.repository

import id.sch.mts241alikhlash.istiqra.StubExamApi
import id.sch.mts241alikhlash.istiqra.api.exam.models.PaperQuestionDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.RecordSessionEventsRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.RecordSessionEventsResponseDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.RecordViolationsRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.ResumeSessionRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.ResumeSessionResponseDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswersBatchDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SavedAnswerDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionPaperDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SubmitSessionRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SubmitSessionResponseDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.ViolationCountDto
import id.sch.mts241alikhlash.istiqra.choices
import id.sch.mts241alikhlash.istiqra.data.local.AnswerDao
import id.sch.mts241alikhlash.istiqra.data.local.CachedAnswerEntity
import id.sch.mts241alikhlash.istiqra.data.local.CachedQuestionEntity
import id.sch.mts241alikhlash.istiqra.data.local.QuestionDao
import id.sch.mts241alikhlash.istiqra.eventsResponse
import id.sch.mts241alikhlash.istiqra.heartbeatOf
import id.sch.mts241alikhlash.istiqra.paperOf
import id.sch.mts241alikhlash.istiqra.submitted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

class QuestionCacheTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `falls back to cache when network fails`() =
        runTest {
            val api = FakeApi(failure = IOException("offline"))
            val dao =
                FakeQuestionDao(
                    preloaded = listOf(cached("q1", cachedAt = now - ExamRepository.CACHE_TTL_MS - 1)),
                )
            val repo = ExamRepository(api, FakeAnswerDao(), dao, now = { now })

            val result = repo.getQuestions("s1")

            assertEquals(listOf("q1"), result.getOrThrow().map { it.id })
            assertEquals("network must have been attempted", 1, api.calls)
        }

    @Test
    fun `forceRefresh falls back to cache when network fails`() =
        runTest {
            val api = FakeApi(failure = IOException("offline"))
            val dao = FakeQuestionDao(preloaded = listOf(cached("q1")))
            val repo = ExamRepository(api, FakeAnswerDao(), dao, now = { now })

            val result = repo.getQuestions("s1", forceRefresh = true)

            assertEquals(listOf("q1"), result.getOrThrow().map { it.id })
            assertEquals(1, api.calls)
        }

    @Test
    fun `network success replaces cache`() =
        runTest {
            val api = FakeApi(questions = listOf(dto("q9")))
            val dao = FakeQuestionDao()
            ExamRepository(api, FakeAnswerDao(), dao).getQuestions("s1")
            assertEquals(listOf("q9"), dao.saved.map { it.id })
        }

    @Test
    fun `empty cache and network failure returns failure`() =
        runTest {
            val repo = ExamRepository(FakeApi(failure = IOException()), FakeAnswerDao(), FakeQuestionDao())
            val result = repo.getQuestions("s1")
            assertTrue(result.isFailure)
            assertEquals(
                ExamRepository.EMPTY_CACHE_MESSAGE,
                result.exceptionOrNull()?.message,
            )
        }

    @Test
    fun `cache hit does not call network`() =
        runTest {
            val api = FakeApi(questions = listOf(dto("q9")))
            val dao = FakeQuestionDao(preloaded = listOf(cached("q1")))
            val result = ExamRepository(api, FakeAnswerDao(), dao, now = { now }).getQuestions("s1")
            assertEquals(listOf("q1"), result.getOrThrow().map { it.id })
            assertEquals(0, api.calls)
        }

    @Test
    fun `sessions do not mix`() =
        runTest {
            val dao =
                FakeQuestionDao(
                    preloaded =
                        listOf(
                            cached("q1", "s1", cachedAt = now - ExamRepository.CACHE_TTL_MS - 1),
                            cached("q2", "s2", cachedAt = now - ExamRepository.CACHE_TTL_MS - 1),
                        ),
                )
            val repo =
                ExamRepository(
                    FakeApi(failure = IOException("offline")),
                    FakeAnswerDao(),
                    dao,
                    now = { now },
                )
            assertEquals(listOf("q1"), repo.getQuestions("s1").getOrThrow().map { it.id })
            assertEquals(listOf("q2"), repo.getQuestions("s2").getOrThrow().map { it.id })
        }

    @Test
    fun `network success replaces only the requested session`() =
        runTest {
            val dao =
                FakeQuestionDao(
                    preloaded = listOf(cached("old", "s1"), cached("other", "s2")),
                )
            val api = FakeApi(questions = listOf(dto("q9")))
            ExamRepository(api, FakeAnswerDao(), dao)
                .getQuestions("s1", forceRefresh = true)
                .getOrThrow()

            assertEquals(listOf("q9"), dao.saved.filter { it.sessionId == "s1" }.map { it.id })
            assertEquals(listOf("other"), dao.saved.filter { it.sessionId == "s2" }.map { it.id })
        }

    @Test
    fun `choices survive cache round trip`() =
        runTest {
            val api = FakeApi(questions = listOf(dto("q1").copy(choices = choices("A", "B"))))
            val dao = FakeQuestionDao()
            ExamRepository(api, FakeAnswerDao(), dao).getQuestions("s1")

            val stored = dao.saved.single()
            assertNotNull(stored.choicesJson)

            val offline =
                ExamRepository(
                    FakeApi(failure = IOException("offline")),
                    FakeAnswerDao(),
                    dao,
                    now = { System.currentTimeMillis() },
                )
            val cachedQuestions = offline.getQuestions("s1").getOrThrow()
            assertEquals(choices("A", "B"), cachedQuestions.single().choices)
        }

    @Test
    fun `empty remote response keeps existing cache`() =
        runTest {
            val dao = FakeQuestionDao(preloaded = listOf(cached("q1")))
            val api = FakeApi(questions = emptyList())
            val result =
                ExamRepository(api, FakeAnswerDao(), dao)
                    .getQuestions("s1", forceRefresh = true)

            assertEquals(listOf("q1"), result.getOrThrow().map { it.id })
            assertEquals(listOf("q1"), dao.saved.map { it.id })
            assertEquals(0, dao.replaceCalls)
        }

    @Test
    fun `stale cache is refreshed from network`() =
        runTest {
            val dao =
                FakeQuestionDao(
                    preloaded =
                        listOf(
                            cached("q1", cachedAt = now - ExamRepository.CACHE_TTL_MS - 1),
                        ),
                )
            val api = FakeApi(questions = listOf(dto("q9")))
            val result = ExamRepository(api, FakeAnswerDao(), dao, now = { now }).getQuestions("s1")

            assertEquals(listOf("q9"), result.getOrThrow().map { it.id })
            assertEquals(1, api.calls)
        }

    @Test
    fun `cache write failure still returns the network result`() =
        runTest {
            val api = FakeApi(questions = listOf(dto("q9")))
            val dao = FakeQuestionDao(failOnReplace = true)
            val logged = mutableListOf<String>()

            val result =
                ExamRepository(
                    api = api,
                    dao = FakeAnswerDao(),
                    questionDao = dao,
                    now = { now },
                    log = { message, _ -> logged.add(message) },
                ).getQuestions("s1")

            assertTrue("cache failure must not fail the load", result.isSuccess)
            assertEquals(listOf("q9"), result.getOrThrow().map { it.id })
            assertEquals(1, logged.size)
        }

    @Test
    fun `cache write failure with empty cache still returns questions`() =
        runTest {
            val dao = FakeQuestionDao(failOnReplace = true)
            val result =
                ExamRepository(
                    FakeApi(questions = listOf(dto("q1"), dto("q2"))),
                    FakeAnswerDao(),
                    dao,
                ).getQuestions("s1")

            assertEquals(listOf("q1", "q2"), result.getOrThrow().map { it.id })
            assertEquals(emptyList<CachedQuestionEntity>(), dao.saved)
        }

    @Test
    fun `clearing a session removes only its cached questions`() =
        runTest {
            val dao =
                FakeQuestionDao(
                    preloaded = listOf(cached("q1", "s1"), cached("q2", "s2")),
                )
            val repo = ExamRepository(FakeApi(), FakeAnswerDao(), dao)

            repo.clearQuestions("s1")

            assertEquals(emptyList<String>(), dao.saved.filter { it.sessionId == "s1" }.map { it.id })
            assertEquals(listOf("q2"), dao.saved.filter { it.sessionId == "s2" }.map { it.id })
        }

    @Test
    fun `cancellation is not swallowed`() =
        runTest {
            val api = FakeApi(failure = CancellationException("cancelled"))
            val repo = ExamRepository(api, FakeAnswerDao(), FakeQuestionDao())

            var caught: Throwable? = null
            try {
                repo.getQuestions("s1")
            } catch (e: CancellationException) {
                caught = e
            }

            assertNotNull(caught)
        }

    @Test
    fun `cache write cancellation is not swallowed`() =
        runTest {
            val dao = FakeQuestionDao(failOnReplace = true, replaceError = CancellationException("stop"))
            val repo = ExamRepository(FakeApi(questions = listOf(dto("q1"))), FakeAnswerDao(), dao)

            var caught: Throwable? = null
            try {
                repo.getQuestions("s1")
            } catch (e: CancellationException) {
                caught = e
            }

            assertNotNull("cancellation during cache write must propagate", caught)
        }

    @Test
    fun cached_question_keeps_its_points_and_choice_keys() =
        runTest {
            val dao = FakeQuestionDao()
            val question = dto("q1").copy(points = 2.5, choices = choices("x", "y"))
            val repo = ExamRepository(FakeApi(questions = listOf(question)), FakeAnswerDao(), dao)

            repo.getQuestions("s1")
            val offline = ExamRepository(FakeApi(failure = IOException("offline")), FakeAnswerDao(), dao)

            assertEquals(listOf(question), offline.getQuestions("s1", forceRefresh = true).getOrThrow())
        }
}

private fun cached(
    id: String,
    sessionId: String = "s1",
    cachedAt: Long = System.currentTimeMillis(),
) = CachedQuestionEntity(
    sessionId = sessionId,
    id = id,
    number = 1,
    type = "MULTIPLE_CHOICE",
    bodyHtml = "<p>Soal $id</p>",
    points = 1.0,
    choicesJson = null,
    cachedAt = cachedAt,
)

private fun dto(id: String) =
    PaperQuestionDto(
        id = id,
        number = 1,
        type = PaperQuestionDto.Type.MULTIPLE_CHOICE,
        bodyHtml = "<p>Soal $id</p>",
        points = 1.0,
        choices = null,
    )

private class FakeQuestionDao(
    preloaded: List<CachedQuestionEntity> = emptyList(),
    private val failOnReplace: Boolean = false,
    private val replaceError: Throwable = IOException("disk full"),
) : QuestionDao {
    private val store = preloaded.toMutableList()
    var replaceCalls = 0
        private set

    val saved: List<CachedQuestionEntity> get() = store.toList()

    override suspend fun replaceForSession(
        sessionId: String,
        items: List<CachedQuestionEntity>,
    ) {
        replaceCalls++
        if (failOnReplace) throw replaceError
        store.removeAll { it.sessionId == sessionId }
        store.addAll(items)
    }

    override suspend fun getForSession(sessionId: String): List<CachedQuestionEntity> = store.filter { it.sessionId == sessionId }.sortedBy { it.number }

    override suspend fun insertAll(items: List<CachedQuestionEntity>) {
        store.addAll(items)
    }

    override suspend fun deleteForSession(sessionId: String) {
        store.removeAll { it.sessionId == sessionId }
    }
}

private class FakeAnswerDao : AnswerDao {
    override fun observeAnswers(sessionId: String): Flow<List<CachedAnswerEntity>> = flowOf(emptyList())

    override suspend fun getAnswer(
        sessionId: String,
        questionId: String,
    ): CachedAnswerEntity? = null

    override suspend fun insertIfAbsent(answer: CachedAnswerEntity): Long = 1L

    override suspend fun getPendingAnswers(sessionId: String): List<CachedAnswerEntity> = emptyList()

    override suspend fun countPendingAnswers(sessionId: String): Int = 0

    override suspend fun countAnswered(sessionId: String): Int = 0

    override suspend fun saveAnswer(answer: CachedAnswerEntity) = Unit

    override suspend fun markConfirmed(
        sessionId: String,
        questionId: String,
        clientId: String,
        updatedAtMillis: Long,
        confirmedAtMillis: Long,
    ): Int = 0

    override suspend fun markSyncing(
        sessionId: String,
        questionId: String,
        clientId: String,
    ): Int = 0

    override suspend fun releaseSyncing(
        sessionId: String,
        questionId: String,
        clientId: String,
    ): Int = 0

    override suspend fun replaceClientId(
        sessionId: String,
        questionId: String,
        clientId: String,
        nextClientId: String,
    ): Int = 0
}

private class FakeApi(
    private val questions: List<PaperQuestionDto> = emptyList(),
    private val failure: Throwable? = null,
) : StubExamApi() {
    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.istiqra.api.exam.models.SessionHeartbeatDto = heartbeatOf()

    override suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    override suspend fun submit(
        sessionId: String,
        request: SubmitSessionRequestDto,
    ): SubmitSessionResponseDto = submitted(SubmitSessionResponseDto.Status.SUBMITTED)

    var calls = 0
        private set

    override suspend fun getQuestions(sessionId: String): SessionPaperDto {
        calls++
        failure?.let { throw it }
        return paperOf(questions)
    }

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchDto,
    ): SaveAnswersBatchResultDto = throw UnsupportedOperationException("not used in QuestionCacheTest")

    override suspend fun recordSessionEvents(
        sessionId: String,
        request: RecordSessionEventsRequestDto,
    ): RecordSessionEventsResponseDto = eventsResponse()

    override suspend fun resumeSession(
        sessionId: String,
        request: ResumeSessionRequestDto,
    ): ResumeSessionResponseDto =
        ResumeSessionResponseDto(
            sessionId = sessionId,
            status = ResumeSessionResponseDto.Status.IN_PROGRESS,
            deadline = "",
            serverTime = "",
            requiresTokenReentry = false,
        )

    override suspend fun reportViolations(
        sessionId: String,
        request: RecordViolationsRequestDto,
    ): ViolationCountDto = ViolationCountDto(request.count)

    override suspend fun reportSuspension(
        sessionId: String,
        request: id.sch.mts241alikhlash.istiqra.api.exam.models.SessionSuspensionRequestDto,
    ): id.sch.mts241alikhlash.istiqra.api.exam.models.SessionSuspensionDto = throw UnsupportedOperationException("not used")
}
