package id.sch.mts241alikhlash.istiqra

import id.sch.mts241alikhlash.istiqra.api.exam.apis.StudentExamsApi
import id.sch.mts241alikhlash.istiqra.api.exam.models.ConfirmedAnswerDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.MyExamDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.PaperChoiceDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.PaperQuestionDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.RecordSessionEventsRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.RecordSessionEventsResponseDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.RecordViolationsRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.ResumeSessionRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.ResumeSessionResponseDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswerDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswerItemDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswersBatchDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SaveAnswersBatchResultDtoSkippedInner
import id.sch.mts241alikhlash.istiqra.api.exam.models.SavedAnswerDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionExamControllerAnswers200Response
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionExamControllerEvents200Response
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionExamControllerHeartbeatNow200Response
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionExamControllerQuestions200Response
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionExamControllerResume200Response
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionExamControllerSave201Response
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionExamControllerSaveBatch200Response
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionExamControllerSubmit200Response
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionExamControllerSuspension201Response
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionExamControllerViolations200Response
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionHeartbeatDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionPaperDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionSuspensionDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SessionSuspensionRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.StartSessionDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.StartedSessionDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.StudentExamControllerFindMine200Response
import id.sch.mts241alikhlash.istiqra.api.exam.models.StudentExamControllerStart201Response
import id.sch.mts241alikhlash.istiqra.api.exam.models.SubmitSessionRequestDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.SubmitSessionResponseDto
import id.sch.mts241alikhlash.istiqra.api.exam.models.ViolationCountDto
import id.sch.mts241alikhlash.istiqra.data.local.AnswerSyncState
import id.sch.mts241alikhlash.istiqra.data.local.CachedAnswerEntity
import id.sch.mts241alikhlash.istiqra.data.session.ServerClock
import kotlinx.coroutines.test.TestCoroutineScheduler
import okhttp3.ResponseBody
import retrofit2.Response

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
fun testClock(scheduler: TestCoroutineScheduler): ServerClock = ServerClock { scheduler.currentTime }.apply { syncEpochMillis(scheduler.currentTime) }

const val TEST_SERVER_TIME = "2026-11-01T01:00:00.000Z"

fun paperOf(questions: List<PaperQuestionDto> = emptyList()) =
    SessionPaperDto(
        questions = questions,
        startedAt = TEST_SERVER_TIME,
        minSubmitMinutes = 0,
        serverTime = TEST_SERVER_TIME,
    )

fun choices(vararg html: String): List<PaperChoiceDto> = html.mapIndexed { index, text -> PaperChoiceDto(PaperChoiceDto.Key.valueOf(('A' + index).toString()), text) }

fun myExam(
    id: String = "e1",
    title: String = "Ujian $id",
    subjectId: String = "s1",
    subjectName: String? = null,
    type: MyExamDto.Type = MyExamDto.Type.ASSIGNMENT,
    category: MyExamDto.Category = if (type == MyExamDto.Type.MIDTERM || type == MyExamDto.Type.FINAL) MyExamDto.Category.EXAM else MyExamDto.Category.NON_EXAM,
    startsAt: String = "2026-11-01T01:00:00.000Z",
    endsAt: String = "2026-11-01T03:00:00.000Z",
    durationMinutes: Int = 90,
    minSubmitMinutes: Int = 0,
    status: MyExamDto.Status = MyExamDto.Status.OPEN,
    sessionId: String? = null,
    activityId: String? = null,
    activityStatus: MyExamDto.ActivityStatus? = null,
    eventId: String? = null,
    eventName: String? = null,
    lifecycle: MyExamDto.Lifecycle =
        when (status) {
            MyExamDto.Status.UPCOMING -> MyExamDto.Lifecycle.UPCOMING
            MyExamDto.Status.FINISHED -> MyExamDto.Lifecycle.FINISHED
            else -> MyExamDto.Lifecycle.OPEN
        },
    roomName: String? = null,
    seatNumber: Int? = null,
    participantNumber: String? = null,
    makeup: Boolean = false,
) = MyExamDto(
    id = id,
    title = title,
    subjectId = subjectId,
    subjectName = subjectName,
    type = type,
    category = category,
    startsAt = startsAt,
    endsAt = endsAt,
    durationMinutes = durationMinutes,
    minSubmitMinutes = minSubmitMinutes,
    status = status,
    sessionId = sessionId,
    activityId = activityId,
    activityStatus = activityStatus,
    eventId = eventId,
    eventName = eventName,
    lifecycle = lifecycle,
    roomName = roomName,
    seatNumber = seatNumber,
    participantNumber = participantNumber,
    makeup = makeup,
)

fun skipped(
    questionId: String,
    clientId: String = "c-$questionId",
) = SaveAnswersBatchResultDtoSkippedInner(questionId = questionId, clientId = clientId)

fun batchAck(
    request: SaveAnswersBatchDto,
    skipped: List<SaveAnswersBatchResultDtoSkippedInner> = emptyList(),
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

fun submitted(
    status: SubmitSessionResponseDto.Status = SubmitSessionResponseDto.Status.SUBMITTED,
    confirmedAnswerCount: Int = 0,
    answersStored: Boolean = true,
    submittedAt: String? = null,
    serverTime: String = TEST_SERVER_TIME,
) = SubmitSessionResponseDto(status, submittedAt, confirmedAnswerCount, answersStored, serverTime)

fun heartbeatOf(
    lockedByViolation: Boolean = false,
    requiresTokenReentry: Boolean = false,
    absentSince: String? = null,
    serverTime: String = TEST_SERVER_TIME,
) = SessionHeartbeatDto(lockedByViolation, requiresTokenReentry, absentSince, serverTime)

fun eventsResponse(
    violationCount: Int = 0,
    requiresTokenReentry: Boolean = false,
    lockedByViolation: Boolean = false,
    serverTime: String = TEST_SERVER_TIME,
) = RecordSessionEventsResponseDto(emptyList(), violationCount, requiresTokenReentry, lockedByViolation, serverTime)

open class InMemoryAnswerDao(
    vararg initial: CachedAnswerEntity,
) : id.sch.mts241alikhlash.istiqra.data.local.AnswerDao {
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
): SaveAnswerItemDto = SaveAnswerItemDto(questionId = questionId, clientId = clientId, answer = emptyMap())

class EmptyQuestionDao : id.sch.mts241alikhlash.istiqra.data.local.QuestionDao {
    private val rows = mutableListOf<id.sch.mts241alikhlash.istiqra.data.local.CachedQuestionEntity>()

    override suspend fun getForSession(sessionId: String) = rows.filter { it.sessionId == sessionId }

    override suspend fun insertAll(items: List<id.sch.mts241alikhlash.istiqra.data.local.CachedQuestionEntity>) {
        rows += items
    }

    override suspend fun deleteForSession(sessionId: String) {
        rows.removeAll { it.sessionId == sessionId }
    }
}

open class StubExamApi : StudentExamsApi {
    val batches = mutableListOf<SaveAnswersBatchDto>()
    val submits = mutableListOf<SubmitSessionRequestDto>()
    val suspensions = mutableListOf<SessionSuspensionRequestDto>()

    var suspensionFailures: Int = 0
    var suspensionDeadline: String = "2026-10-27T06:00:00Z"
    var suspensionExtraMinutes: Int = 0

    open suspend fun reportSuspension(
        sessionId: String,
        request: SessionSuspensionRequestDto,
    ): SessionSuspensionDto {
        suspensions += request
        if (suspensionFailures > 0) {
            suspensionFailures -= 1
            throw id.sch.mts241alikhlash.istiqra.data.remote
                .ApiException(0, "offline")
        }
        return SessionSuspensionDto(
            deadline = suspensionDeadline,
            finalizeAt = suspensionDeadline,
            extraMinutes = suspensionExtraMinutes,
            suspendedSeconds = request.seconds,
        )
    }

    open suspend fun sessionHeartbeat(sessionId: String): SessionHeartbeatDto = heartbeatOf()

    open suspend fun getQuestions(sessionId: String): SessionPaperDto = paperOf()

    open suspend fun getAnswers(sessionId: String): List<SavedAnswerDto> = emptyList()

    open suspend fun saveAnswersBatch(
        sessionId: String,
        request: SaveAnswersBatchDto,
    ): SaveAnswersBatchResultDto {
        batches += request
        return batchAck(request)
    }

    open suspend fun submit(
        sessionId: String,
        request: SubmitSessionRequestDto,
    ): SubmitSessionResponseDto {
        submits += request
        return submitted()
    }

    open suspend fun recordSessionEvents(
        sessionId: String,
        request: RecordSessionEventsRequestDto,
    ): RecordSessionEventsResponseDto = eventsResponse()

    open suspend fun resumeSession(
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

    open suspend fun reportViolations(
        sessionId: String,
        request: RecordViolationsRequestDto,
    ): ViolationCountDto = ViolationCountDto(request.count)

    override suspend fun sessionExamControllerAnswers(
        xDeviceId: String,
        sessionId: String,
        xClientPlatform: String?,
    ) = Response.success(SessionExamControllerAnswers200Response(data = getAnswers(sessionId)))

    override suspend fun sessionExamControllerEvents(
        xDeviceId: String,
        sessionId: String,
        recordSessionEventsRequestDto: RecordSessionEventsRequestDto,
        xClientPlatform: String?,
    ) = Response.success(SessionExamControllerEvents200Response(data = recordSessionEvents(sessionId, recordSessionEventsRequestDto)))

    override suspend fun sessionExamControllerFile(
        xDeviceId: String,
        sessionId: String,
        fileId: String,
        xClientPlatform: String?,
    ): Response<ResponseBody> = throw UnsupportedOperationException()

    override suspend fun sessionExamControllerHeartbeatNow(
        xDeviceId: String,
        sessionId: String,
        xClientPlatform: String?,
    ) = Response.success(SessionExamControllerHeartbeatNow200Response(data = sessionHeartbeat(sessionId)))

    override suspend fun sessionExamControllerQuestions(
        xDeviceId: String,
        sessionId: String,
        xClientPlatform: String?,
    ) = Response.success(SessionExamControllerQuestions200Response(data = getQuestions(sessionId)))

    override suspend fun sessionExamControllerResume(
        xDeviceId: String,
        sessionId: String,
        resumeSessionRequestDto: ResumeSessionRequestDto,
        xClientPlatform: String?,
    ) = Response.success(SessionExamControllerResume200Response(data = resumeSession(sessionId, resumeSessionRequestDto)))

    override suspend fun sessionExamControllerSave(
        xDeviceId: String,
        sessionId: String,
        saveAnswerDto: SaveAnswerDto,
        xClientPlatform: String?,
    ): Response<SessionExamControllerSave201Response> = throw UnsupportedOperationException()

    override suspend fun sessionExamControllerSaveBatch(
        xDeviceId: String,
        sessionId: String,
        saveAnswersBatchDto: SaveAnswersBatchDto,
        xClientPlatform: String?,
    ) = Response.success(SessionExamControllerSaveBatch200Response(data = saveAnswersBatch(sessionId, saveAnswersBatchDto)))

    override suspend fun sessionExamControllerSubmit(
        xDeviceId: String,
        sessionId: String,
        submitSessionRequestDto: SubmitSessionRequestDto,
        xClientPlatform: String?,
    ) = Response.success(SessionExamControllerSubmit200Response(data = submit(sessionId, submitSessionRequestDto)))

    override suspend fun sessionExamControllerSuspension(
        xDeviceId: String,
        sessionId: String,
        sessionSuspensionRequestDto: SessionSuspensionRequestDto,
        xClientPlatform: String?,
    ) = Response.success(SessionExamControllerSuspension201Response(data = reportSuspension(sessionId, sessionSuspensionRequestDto)))

    override suspend fun sessionExamControllerViolations(
        xDeviceId: String,
        sessionId: String,
        recordViolationsRequestDto: RecordViolationsRequestDto,
        xClientPlatform: String?,
    ) = Response.success(SessionExamControllerViolations200Response(data = reportViolations(sessionId, recordViolationsRequestDto)))

    open suspend fun listExams(): List<MyExamDto> = emptyList()

    open suspend fun startSession(
        examId: String,
        request: StartSessionDto,
    ): StartedSessionDto = throw UnsupportedOperationException()

    override suspend fun studentExamControllerFindMine() = Response.success(StudentExamControllerFindMine200Response(data = listExams()))

    override suspend fun studentExamControllerStart(
        scheduleId: String,
        startSessionDto: StartSessionDto,
    ) = Response.success(StudentExamControllerStart201Response(data = startSession(scheduleId, startSessionDto)))
}

fun paperQuestion(questionId: String = "q1") =
    PaperQuestionDto(
        id = questionId,
        number = 1,
        type = PaperQuestionDto.Type.MULTIPLE_CHOICE,
        bodyHtml = "<p>Soal</p>",
        points = 1.0,
        choices = choices("A", "B"),
    )

open class PaperExamApi : StubExamApi() {
    override suspend fun getQuestions(sessionId: String) = paperOf(listOf(paperQuestion()))
}
