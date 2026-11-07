package id.sch.mts241alikhlash.exam.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.sch.mts241alikhlash.exam.data.local.AnswerSyncState
import id.sch.mts241alikhlash.exam.data.model.AnswerPayload
import id.sch.mts241alikhlash.exam.data.model.PaperQuestionDto
import id.sch.mts241alikhlash.exam.data.remote.ApiErrorCode
import id.sch.mts241alikhlash.exam.data.remote.ApiException
import id.sch.mts241alikhlash.exam.data.repository.ExamRepository
import id.sch.mts241alikhlash.exam.data.repository.isSessionLost
import id.sch.mts241alikhlash.exam.data.session.ExamTimer
import id.sch.mts241alikhlash.exam.data.session.ServerClock
import id.sch.mts241alikhlash.exam.data.session.parseIsoMillis
import id.sch.mts241alikhlash.exam.kiosk.ViolationPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnswerSyncSummary(
    val totalAnswered: Int = 0,
    val confirmedCount: Int = 0,
    val pendingCount: Int = 0,
) {
    val allConfirmed: Boolean get() = pendingCount == 0
}

data class ExamUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val questions: List<PaperQuestionDto> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<String, AnswerPayload> = emptyMap(),
    val drafts: Map<String, String> = emptyMap(),
    val doubtfuls: Set<String> = emptySet(),
    val remainingSeconds: Long = ExamTimer.EXAM_DURATION_SECONDS,
    val isSubmitted: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val isResumed: Boolean = false,
    val isTimeOver: Boolean = false,
    val violationExit: Boolean = false,
    val deliveryWarning: String? = null,
    val lostMessage: String? = null,
    val unsentAnswers: Int = 0,
    val sync: AnswerSyncSummary = AnswerSyncSummary(),
    val serverViolations: Int = 0,
    val minSubmitMinutes: Int = 0,
    val submitGateSeconds: Long = 0,
) {
    val canAnswer: Boolean get() = !isLoading && !isSubmitting && !isSubmitted && !isTimeOver && questions.isNotEmpty()

    val canSubmit: Boolean
        get() = !isLoading && !isSubmitting && !isSubmitted && questions.isNotEmpty() && submitGateSeconds == 0L

    val submitGateMinutes: Long get() = (submitGateSeconds + 59L) / 60L

    val pendingSync: Int get() = sync.pendingCount
}

class ExamViewModel(
    private val repository: ExamRepository,
    private val sessionId: String,
    deadline: Long? = null,
    restored: Boolean? = null,
    private val clock: ServerClock = ServerClock(),
    private val onSubmitConfirmed: suspend (String) -> Unit = {},
    private val isOnline: () -> Boolean = { true },
    private val batteryLevel: () -> Int? = { null },
    private val isCharging: () -> Boolean? = { null },
) : ViewModel() {
    private val _uiState =
        MutableStateFlow(
            ExamUiState(
                remainingSeconds =
                    deadline?.let { ExamTimer.remainingSeconds(it, clock.nowMillis()) }
                        ?: ExamTimer.EXAM_DURATION_SECONDS,
                isResumed = deadline != null && ExamTimer.isResumed(deadline, clock.nowMillis(), restored),
            ),
        )
    val uiState: StateFlow<ExamUiState> = _uiState.asStateFlow()

    private val timerDeadline: Long = deadline ?: ExamTimer.deadlineFrom(clock.nowMillis())
    private var startedAtMillis: Long? = null
    private var timerJob: Job? = null
    private var debounceJob: Job? = null

    init {
        loadQuestions()
        observeLocalAnswers()
        startTimer()
    }

    fun loadQuestions(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository
                .loadPaper(sessionId, forceRefresh)
                .onSuccess { paper ->
                    startedAtMillis =
                        paper.startedAt
                            .takeIf { it.isNotBlank() }
                            ?.let { runCatching { parseIsoMillis(it) }.getOrNull() }
                    _uiState.value =
                        _uiState.value.copy(
                            minSubmitMinutes = paper.minSubmitMinutes,
                            submitGateSeconds = submitGate(paper.minSubmitMinutes),
                        )
                    val list = paper.questions
                    if (list.isEmpty()) {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                questions = emptyList(),
                                errorMessage = EMPTY_PAPER_MESSAGE,
                            )
                    } else {
                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                questions = list,
                                currentIndex =
                                    _uiState.value.currentIndex
                                        .coerceIn(0, list.lastIndex),
                                errorMessage = null,
                            )
                    }
                }.onFailure { err ->
                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            questions = emptyList(),
                            errorMessage = err.localizedMessage ?: "Gagal memuat soal",
                        )
                }
        }
    }

    fun retry() = loadQuestions(forceRefresh = true)

    private fun submitGate(minSubmitMinutes: Int): Long {
        val startedAt = startedAtMillis ?: return 0L
        return ExamTimer.secondsUntilSubmitAllowed(
            startedAt,
            minSubmitMinutes,
            timerDeadline,
            clock.nowMillis(),
        )
    }

    private fun observeLocalAnswers() {
        viewModelScope.launch {
            repository.observeAnswers(sessionId).collect { list ->
                val answerMap =
                    list
                        .mapNotNull { row ->
                            AnswerPayload.decode(row.answer)?.let { row.questionId to it }
                        }.toMap()
                val doubtSet = list.filter { it.isDoubtful }.map { it.questionId }.toSet()
                val answered =
                    list.filter {
                        it.answer.isNotBlank() && AnswerPayload.decode(it.answer) != null
                    }
                _uiState.value =
                    _uiState.value.copy(
                        answers = answerMap,
                        doubtfuls = doubtSet,
                        sync =
                            AnswerSyncSummary(
                                totalAnswered = answered.size,
                                confirmedCount = answered.count { it.syncState == AnswerSyncState.SERVER_CONFIRMED },
                                pendingCount = answered.count { it.syncState != AnswerSyncState.SERVER_CONFIRMED },
                            ),
                    )
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob =
            viewModelScope.launch {
                var elapsedMs = 0L
                while (!_uiState.value.isSubmitted) {
                    val remaining = ExamTimer.remainingSeconds(timerDeadline, clock.nowMillis())
                    _uiState.value =
                        _uiState.value.copy(
                            remainingSeconds = remaining,
                            submitGateSeconds = submitGate(_uiState.value.minSubmitMinutes),
                        )
                    if (remaining <= 0L) {
                        autoSubmit()
                        break
                    }
                    if (elapsedMs >= SYNC_INTERVAL_MS) {
                        elapsedMs = 0L
                        launch { syncNow() }
                    }
                    elapsedMs += TICK_MS
                    delay(TICK_MS)
                }
            }
    }

    fun onConnectivityChanged(online: Boolean) {
        if (!online || _uiState.value.isSubmitted) return
        debounceJob?.cancel()
        debounceJob =
            viewModelScope.launch {
                delay(SYNC_DEBOUNCE_MS)
                syncNow()
            }
    }

    private suspend fun syncNow() {
        val state = _uiState.value
        if (state.isSubmitted || state.isSubmitting || state.sync.pendingCount == 0) return
        if (!isOnline()) return
        val outcome =
            runCatching {
                repository.syncPendingBatch(sessionId, batteryLevel(), isCharging())
            }.getOrElse { return }
        val closed = outcome.closedMessage
        if (closed != null) {
            _uiState.value =
                if (
                    outcome.closedCode == ApiErrorCode.WINDOW_CLOSED || closed == WINDOW_CLOSED_MESSAGE
                ) {
                    _uiState.value.copy(deliveryWarning = closed)
                } else {
                    _uiState.value.copy(lostMessage = closed)
                }
        } else if (outcome.rejectedMessage != null) {
            _uiState.value = _uiState.value.copy(deliveryWarning = outcome.rejectedMessage)
        }
    }

    private var violations = 0
    private var violationSubmitStarted = false

    private var reportedViolations = -1
    private var reportingViolations = false

    private fun reportViolations() {
        val count = violations
        if (count <= reportedViolations || reportingViolations) return
        reportingViolations = true
        viewModelScope.launch {
            val server = repository.reportViolations(sessionId, count)
            reportingViolations = false
            if (server == null) return@launch
            reportedViolations = count
            if (server > _uiState.value.serverViolations) {
                _uiState.value = _uiState.value.copy(serverViolations = server)
            }
            reportViolations()
        }
    }

    fun recordViolations(count: Int) {
        violations = maxOf(violations, count)
        reportViolations()
        if (!ViolationPolicy.exceeded(violations) || violationSubmitStarted) return
        val state = _uiState.value
        if (state.isSubmitted || state.lostMessage != null) return
        violationSubmitStarted = true
        _uiState.value = state.copy(violationExit = true)
        viewModelScope.launch { retrySubmit() }
    }

    private suspend fun autoSubmit() {
        _uiState.value = _uiState.value.copy(isTimeOver = true)
        retrySubmit()
    }

    private suspend fun retrySubmit() {
        var attempts = 0
        while (!_uiState.value.isSubmitted && _uiState.value.lostMessage == null && attempts < AUTO_SUBMIT_MAX_ATTEMPTS) {
            if (!_uiState.value.isSubmitting) {
                attempts++
                if (runSubmit()) break
            }
            delay(AUTO_SUBMIT_RETRY_MS)
        }
    }

    private val optimistic = mutableMapOf<String, AnswerPayload>()
    private val optimisticDoubt = mutableMapOf<String, Boolean>()
    private val textJobs = mutableMapOf<String, Job>()

    private fun isDoubtful(questionId: String): Boolean = optimisticDoubt[questionId] ?: _uiState.value.doubtfuls.contains(questionId)

    private fun currentAnswer(questionId: String): AnswerPayload? = optimistic[questionId] ?: _uiState.value.answers[questionId]

    fun selectAnswer(
        questionId: String,
        answer: AnswerPayload,
    ) {
        if (!_uiState.value.canAnswer) return
        optimistic[questionId] = answer
        val currentDoubt = isDoubtful(questionId)
        viewModelScope.launch {
            repository.recordAnswer(questionId, sessionId, answer.encode(), currentDoubt)
            scheduleSync()
        }
    }

    fun toggleKey(
        questionId: String,
        key: String,
    ) {
        val current = (currentAnswer(questionId) as? AnswerPayload.Multiple)?.keys.orEmpty()
        val next = if (key in current) current - key else current + key
        selectAnswer(questionId, AnswerPayload.Multiple(next.sorted()))
    }

    fun updateText(
        questionId: String,
        text: String,
    ) {
        if (!_uiState.value.canAnswer) return
        val clamped = text.take(MAX_TEXT_LENGTH)
        _uiState.value = _uiState.value.copy(drafts = _uiState.value.drafts + (questionId to clamped))
        textJobs[questionId]?.cancel()
        textJobs[questionId] =
            viewModelScope.launch {
                delay(TEXT_DEBOUNCE_MS)
                commitDraft(questionId)
            }
    }

    private fun scheduleSync() {
        debounceJob?.cancel()
        debounceJob =
            viewModelScope.launch {
                delay(SYNC_DEBOUNCE_MS)
                syncNow()
            }
    }

    private suspend fun commitDraft(questionId: String) {
        val text = _uiState.value.drafts[questionId] ?: return
        val payload = AnswerPayload.Text(text)
        optimistic[questionId] = payload
        val doubt = isDoubtful(questionId)
        repository.recordAnswer(questionId, sessionId, payload.encode(), doubt)
        scheduleSync()
        if (_uiState.value.drafts[questionId] == text) {
            _uiState.value = _uiState.value.copy(drafts = _uiState.value.drafts - questionId)
        }
    }

    private suspend fun flushDrafts() {
        for (questionId in _uiState.value.drafts.keys
            .toList()) {
            textJobs.remove(questionId)?.cancel()
            commitDraft(questionId)
        }
    }

    fun toggleDoubtful(questionId: String) {
        if (!_uiState.value.canAnswer) return
        val currentAnswer = currentAnswer(questionId)?.encode() ?: ""
        val newDoubt = !isDoubtful(questionId)
        optimisticDoubt[questionId] = newDoubt
        viewModelScope.launch {
            repository.recordAnswer(questionId, sessionId, currentAnswer, newDoubt)
            scheduleSync()
        }
    }

    fun goToIndex(index: Int) {
        if (index in _uiState.value.questions.indices) {
            _uiState.value = _uiState.value.copy(currentIndex = index)
            viewModelScope.launch { flushDrafts() }
        }
    }

    fun submitExam() {
        val state = _uiState.value
        if (state.isSubmitting || state.isSubmitted || !state.canSubmit) return

        viewModelScope.launch { runSubmit() }
    }

    private suspend fun runSubmit(): Boolean {
        _uiState.value = _uiState.value.copy(isSubmitting = true, submitError = null)
        return try {
            flushDrafts()
            val result = repository.submit(sessionId, violations)
            onSubmitConfirmed(sessionId)
            _uiState.value =
                _uiState.value.copy(
                    isSubmitting = false,
                    isSubmitted = true,
                    unsentAnswers = if (result.answersStored) 0 else _uiState.value.sync.pendingCount,
                    deliveryWarning =
                        if (result.answersStored) {
                            _uiState.value.deliveryWarning
                        } else {
                            ANSWERS_NOT_STORED_MESSAGE
                        },
                )
            true
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            val api = error as? ApiException
            val sessionLost = api?.takeIf { it.statusCode == 409 && isSessionLost(it) }
            _uiState.value =
                if (sessionLost != null) {
                    _uiState.value.copy(isSubmitting = false, lostMessage = sessionLost.message)
                } else if (api?.code == ApiErrorCode.MIN_SUBMIT) {
                    _uiState.value.copy(
                        isSubmitting = false,
                        submitGateSeconds = submitGate(_uiState.value.minSubmitMinutes),
                        submitError = api.message,
                    )
                } else {
                    val unsent = _uiState.value.sync.pendingCount
                    _uiState.value.copy(
                        isSubmitting = false,
                        unsentAnswers = unsent,
                        submitError = api?.message?.takeIf { it.isNotBlank() } ?: SUBMIT_ERROR_MESSAGE,
                    )
                }
            false
        }
    }

    companion object {
        const val EMPTY_PAPER_MESSAGE = "Soal ujian tidak tersedia. Periksa koneksi lalu coba lagi."
        const val WINDOW_CLOSED_MESSAGE = "Waktu pengiriman jawaban sudah habis"
        const val ANSWERS_NOT_STORED_MESSAGE =
            "Ujian sudah berakhir di server. Jawaban terakhir belum tersimpan."
        const val TEXT_DEBOUNCE_MS = 600L
        const val MAX_TEXT_LENGTH = 20_000
        const val AUTO_SUBMIT_RETRY_MS = 5_000L
        const val AUTO_SUBMIT_MAX_ATTEMPTS = 120
        const val SUBMIT_ERROR_MESSAGE = "Gagal mengumpulkan jawaban. Coba lagi."
        const val TICK_MS = 1_000L
        const val SYNC_INTERVAL_MS = 10_000L
        const val SYNC_DEBOUNCE_MS = 1_500L
    }
}
