package id.sch.mts241alikhlash.exam

import id.sch.mts241alikhlash.exam.data.local.AnswerSyncState
import id.sch.mts241alikhlash.exam.data.local.CachedAnswerEntity
import id.sch.mts241alikhlash.exam.data.model.ConfirmedAnswerDto
import id.sch.mts241alikhlash.exam.data.model.PaperChoiceDto
import id.sch.mts241alikhlash.exam.data.model.RecordSessionEventsRequest
import id.sch.mts241alikhlash.exam.data.model.RecordSessionEventsResponse
import id.sch.mts241alikhlash.exam.data.model.ResumeSessionRequest
import id.sch.mts241alikhlash.exam.data.model.ResumedSessionDto
import id.sch.mts241alikhlash.exam.data.model.SaveAnswerItemDto
import id.sch.mts241alikhlash.exam.data.model.SaveAnswersBatchRequest
import id.sch.mts241alikhlash.exam.data.model.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.exam.data.model.SkippedAnswerDto
import id.sch.mts241alikhlash.exam.data.model.SuspensionRequest
import id.sch.mts241alikhlash.exam.data.model.SuspensionResponseDto
import id.sch.mts241alikhlash.exam.data.model.ViolationCountDto
import id.sch.mts241alikhlash.exam.data.model.ViolationReport
import id.sch.mts241alikhlash.exam.data.session.ServerClock
import kotlinx.coroutines.test.TestCoroutineScheduler

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
fun testClock(scheduler: TestCoroutineScheduler): ServerClock = ServerClock { scheduler.currentTime }.apply { syncEpochMillis(scheduler.currentTime) }

fun paperOf(questions: List<id.sch.mts241alikhlash.exam.data.model.PaperQuestionDto> = emptyList()) =
    id.sch.mts241alikhlash.exam.data.model.SessionPaperDto(
        questions = questions,
        startedAt = "2026-11-01T01:00:00.000Z",
        minSubmitMinutes = 0,
        serverTime = "2026-11-01T01:00:00.000Z",
    )

fun choices(vararg html: String): List<PaperChoiceDto> = html.mapIndexed { index, text -> PaperChoiceDto(('A' + index).toString(), text) }

fun batchAck(
    request: SaveAnswersBatchRequest,
    skipped: List<SkippedAnswerDto> = emptyList(),
) = SaveAnswersBatchResultDto(
    confirmed =
        request.answers
            .filterNot { item -> skipped.any { it.questionId == item.questionId && it.clientId == item.clientId } }
            .map {
                ConfirmedAnswerDto(
                    questionId = it.questionId,
                    clientId = it.clientId,
                    updatedAt = "2026-10-27T00:00:00Z",
                    receivedAfterDeadline = false,
                )
            },
    skipped = skipped,
    confirmedCount = request.answers.size,
    serverTime = "2026-10-27T00:00:00Z",
)

open class InMemoryAnswerDao(
    vararg initial: CachedAnswerEntity,
) : id.sch.mts241alikhlash.exam.data.local.AnswerDao {
    val rows = initial.associateBy { it.questionId }.toMutableMap()

    private val changes = kotlinx.coroutines.flow.MutableStateFlow(0)

    override fun observeAnswers(sessionId: String): kotlinx.coroutines.flow.Flow<List<CachedAnswerEntity>> =
        kotlinx.coroutines.flow.flow {
            changes.collect { emit(rows.values.filter { it.sessionId == sessionId }) }
        }

    override suspend fun getAnswer(
        sessionId: String,
        questionId: String,
    ) = rows[questionId]?.takeIf { it.sessionId == sessionId }

    override suspend fun getPendingAnswers(sessionId: String) =
        rows.values
            .filter { it.sessionId == sessionId && it.syncState != AnswerSyncState.SERVER_CONFIRMED }
            .sortedBy { it.updatedAtMillis }

    override suspend fun countPendingAnswers(sessionId: String) = rows.values.count { it.sessionId == sessionId && it.syncState != AnswerSyncState.SERVER_CONFIRMED }

    override suspend fun countAnswered(sessionId: String) = rows.values.count { it.sessionId == sessionId && it.answer.isNotBlank() }

    override suspend fun saveAnswer(answer: CachedAnswerEntity) {
        rows[answer.questionId] = answer
        changes.value++
    }

    override suspend fun insertIfAbsent(answer: CachedAnswerEntity): Long {
        if (rows.containsKey(answer.questionId)) return -1L
        rows[answer.questionId] = answer
        changes.value++
        return 1L
    }

    override suspend fun markConfirmed(
        sessionId: String,
        questionId: String,
        clientId: String,
        updatedAtMillis: Long,
        confirmedAtMillis: Long,
    ): Int {
        val row = rows[questionId] ?: return 0
        if (
            row.sessionId != sessionId ||
            row.clientId != clientId ||
            row.updatedAtMillis != updatedAtMillis ||
            row.syncState == AnswerSyncState.SERVER_CONFIRMED
        ) {
            return 0
        }
        rows[questionId] =
            row.copy(
                syncState = AnswerSyncState.SERVER_CONFIRMED,
                confirmedAtMillis = confirmedAtMillis,
            )
        changes.value++
        return 1
    }

    override suspend fun markSyncing(
        sessionId: String,
        questionId: String,
        clientId: String,
    ): Int {
        val row = rows[questionId] ?: return 0
        if (
            row.sessionId != sessionId ||
            row.clientId != clientId ||
            row.syncState == AnswerSyncState.SERVER_CONFIRMED
        ) {
            return 0
        }
        rows[questionId] = row.copy(syncState = AnswerSyncState.SYNCING)
        changes.value++
        return 1
    }

    override suspend fun releaseSyncing(
        sessionId: String,
        questionId: String,
        clientId: String,
    ): Int {
        val row = rows[questionId] ?: return 0
        if (row.sessionId != sessionId || row.clientId != clientId || row.syncState != AnswerSyncState.SYNCING) {
            return 0
        }
        rows[questionId] = row.copy(syncState = AnswerSyncState.LOCAL_PENDING)
        changes.value++
        return 1
    }

    override suspend fun replaceClientId(
        sessionId: String,
        questionId: String,
        clientId: String,
        nextClientId: String,
    ): Int {
        val row = rows[questionId] ?: return 0
        if (row.sessionId != sessionId || row.clientId != clientId) return 0
        rows[questionId] = row.copy(clientId = nextClientId)
        changes.value++
        return 1
    }
}

fun pendingAnswer(
    questionId: String,
    sessionId: String = "s1",
    answer: String = """{"key":"A"}""",
    updatedAtMillis: Long = 1_000L,
    clientId: String = "c-$questionId",
) = CachedAnswerEntity(
    questionId = questionId,
    sessionId = sessionId,
    answer = answer,
    clientId = clientId,
    syncState = AnswerSyncState.LOCAL_PENDING,
    updatedAtMillis = updatedAtMillis,
)

fun item(
    questionId: String,
    clientId: String = "c-$questionId",
): SaveAnswerItemDto = SaveAnswerItemDto(questionId = questionId, clientId = clientId, answer = kotlinx.serialization.json.JsonObject(emptyMap()))

class EmptyQuestionDao : id.sch.mts241alikhlash.exam.data.local.QuestionDao {
    private val rows = mutableListOf<id.sch.mts241alikhlash.exam.data.local.CachedQuestionEntity>()

    override suspend fun getForSession(sessionId: String) = rows.filter { it.sessionId == sessionId }

    override suspend fun insertAll(items: List<id.sch.mts241alikhlash.exam.data.local.CachedQuestionEntity>) {
        rows += items
    }

    override suspend fun deleteForSession(sessionId: String) {
        rows.removeAll { it.sessionId == sessionId }
    }
}

open class StubExamApi : id.sch.mts241alikhlash.exam.data.remote.ExamApiService {
    val batches = mutableListOf<SaveAnswersBatchRequest>()
    val submits = mutableListOf<id.sch.mts241alikhlash.exam.data.model.SubmitRequest>()
    val suspensions = mutableListOf<SuspensionRequest>()

    var suspensionFailures: Int = 0
    var suspensionDeadline: String = "2026-10-27T06:00:00Z"
    var suspensionExtraMinutes: Int = 0

    override suspend fun reportSuspension(
        sessionId: String,
        request: SuspensionRequest,
    ): SuspensionResponseDto {
        suspensions += request
        if (suspensionFailures > 0) {
            suspensionFailures -= 1
            throw id.sch.mts241alikhlash.exam.data.remote
                .ApiException(0, "offline")
        }
        return SuspensionResponseDto(
            deadline = suspensionDeadline,
            finalizeAt = suspensionDeadline,
            extraMinutes = suspensionExtraMinutes,
            suspendedSeconds = request.seconds,
        )
    }

    override suspend fun sessionHeartbeat(
        sessionId: String,
    ): id.sch.mts241alikhlash.exam.data.model.SessionHeartbeatResponse =
        id.sch.mts241alikhlash.exam.data.model
            .SessionHeartbeatResponse()

    override suspend fun getQuestions(sessionId: String) = paperOf()

    override suspend fun getAnswers(sessionId: String): List<id.sch.mts241alikhlash.exam.data.model.SavedAnswerDto> = emptyList()

    override suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchRequest,
    ): SaveAnswersBatchResultDto {
        batches += request
        return batchAck(request)
    }

    override suspend fun submit(
        sessionId: String,
        request: id.sch.mts241alikhlash.exam.data.model.SubmitRequest,
    ): id.sch.mts241alikhlash.exam.data.model.SubmittedSessionDto {
        submits += request
        return id.sch.mts241alikhlash.exam.data.model
            .SubmittedSessionDto("SUBMITTED")
    }

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
}

fun paperQuestion(questionId: String = "q1") =
    id.sch.mts241alikhlash.exam.data.model.PaperQuestionDto(
        id = questionId,
        number = 1,
        type = "MULTIPLE_CHOICE",
        bodyHtml = "<p>Soal</p>",
        points = 1.0,
        choices = choices("A", "B"),
    )

open class PaperExamApi : StubExamApi() {
    override suspend fun getQuestions(sessionId: String) = paperOf(listOf(paperQuestion()))
}
