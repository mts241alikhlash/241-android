package id.sch.mts241alikhlash.cbt.ui.screens

import id.sch.mts241alikhlash.cbt.proctor.CameraPermissionState
import id.sch.mts241alikhlash.cbt.proctor.ProctorController
import id.sch.mts241alikhlash.cbt.proctor.rememberProctorPermission
import id.sch.mts241alikhlash.cbt.ui.exam.ImageContext
import id.sch.mts241alikhlash.cbt.ui.exam.QuestionAnswerInput
import id.sch.mts241alikhlash.cbt.ui.exam.isFilled
import id.sch.mts241alikhlash.cbt.ui.exam.HtmlContent
import id.sch.mts241alikhlash.cbt.data.model.AnswerPayload
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import id.sch.mts241alikhlash.cbt.ui.components.BrandWatermark
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import id.sch.mts241alikhlash.cbt.data.model.PaperQuestionDto
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import id.sch.mts241alikhlash.cbt.ui.components.ActionButton
import id.sch.mts241alikhlash.cbt.ui.components.ActionStyle
import id.sch.mts241alikhlash.cbt.ui.components.AppDialog
import id.sch.mts241alikhlash.cbt.data.model.ExamSummaryDto
import id.sch.mts241alikhlash.cbt.ui.components.CardHeader
import id.sch.mts241alikhlash.cbt.ui.components.SectionHeader
import id.sch.mts241alikhlash.cbt.ui.components.SectionedCard
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.cbt.R
import id.sch.mts241alikhlash.cbt.kiosk.ViolationPolicy
import id.sch.mts241alikhlash.cbt.ui.theme.BorderFocus
import id.sch.mts241alikhlash.cbt.ui.theme.BorderSubtle
import id.sch.mts241alikhlash.cbt.ui.theme.BrandBackground
import id.sch.mts241alikhlash.cbt.ui.theme.BrandMuted
import id.sch.mts241alikhlash.cbt.ui.theme.BrandNavy
import id.sch.mts241alikhlash.cbt.ui.theme.BrandNavySubtle
import id.sch.mts241alikhlash.cbt.ui.theme.BrandSurface
import id.sch.mts241alikhlash.cbt.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.cbt.ui.theme.DoubtfulOrange
import id.sch.mts241alikhlash.cbt.ui.theme.DoubtfulOrangeLight
import id.sch.mts241alikhlash.cbt.ui.theme.StatusError
import id.sch.mts241alikhlash.cbt.ui.theme.StatusSuccess
import id.sch.mts241alikhlash.cbt.ui.theme.StatusSuccessLight
import id.sch.mts241alikhlash.cbt.ui.theme.SubmitGreen
import id.sch.mts241alikhlash.cbt.ui.theme.TextMuted
import id.sch.mts241alikhlash.cbt.ui.theme.TextPrimary
import id.sch.mts241alikhlash.cbt.ui.theme.TextSecondary

@Composable
fun ExamScreen(
    viewModel: ExamViewModel,
    sessionId: String,
    exam: ExamSummaryDto? = null,
    baseUrl: String,
    imageLoader: coil3.ImageLoader?,
    onFinish: () -> Unit,
    onSupervisorExit: () -> Unit = {},
    onSessionLost: (String) -> Unit = {},
    proctor: ProctorController? = null
) {
    val state by viewModel.uiState.collectAsState()

    val isOnline = id.sch.mts241alikhlash.cbt.ui.components.rememberIsOnline()
    LaunchedEffect(isOnline) { viewModel.onConnectivityChanged(isOnline) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    if (proctor != null) {
        rememberProctorPermission(sessionId, proctor)
    }

    DisposableEffect(proctor) {
        onDispose { proctor?.stop() }
    }
    androidx.compose.foundation.layout.Box {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopStart)
                .size(64.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onSupervisorExit() },
                    )
                },
        )
        androidx.compose.foundation.layout.Column {
        ExamContent(
        state = state,
        onAnswer = viewModel::selectAnswer,
        onToggleKey = viewModel::toggleKey,
        onTextChange = viewModel::updateText,
        onToggleDoubtful = viewModel::toggleDoubtful,
        onGoToIndex = viewModel::goToIndex,
        onSubmit = viewModel::submitExam,
        onRetry = viewModel::retry,
        onFinish = onFinish,
        onSupervisorExit = onSupervisorExit,
        onSessionLost = onSessionLost,
        sessionId = sessionId,
        exam = exam,
        baseUrl = baseUrl,
        imageLoader = imageLoader
        )
        }
    }
}

data class SubmitSummary(
    val total: Int,
    val answered: Int,
    val doubtful: Int,
    val unanswered: Int,
    val pendingSync: Int
)

fun submitSummary(state: ExamUiState): SubmitSummary {
    val total = state.questions.size
    val answered = state.questions.count { q ->
        state.answers[q.id].isFilled() || !state.drafts[q.id].isNullOrBlank()
    }
    return SubmitSummary(
        total = total,
        answered = answered,
        doubtful = state.doubtfuls.size,
        unanswered = (total - answered).coerceAtLeast(0),
        pendingSync = state.pendingSync
    )
}

@Composable
private fun SyncIndicator(summary: AnswerSyncSummary) {
    if (summary.allConfirmed) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(DoubtfulOrange, CircleShape)
        )
        Text(
            text = "Menyinkronkan ${summary.pendingCount}",
            fontFamily = DMSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = DoubtfulOrange
        )
    }
}

@Composable
private fun SubmitCountRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = DMSansFontFamily,
            fontSize = 13.sp,
            color = TextPrimary
        )
        Text(
            text = count.toString(),
            fontFamily = DMSansFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = color
        )
    }
}

@Composable
fun ExamContent(
    state: ExamUiState,
    onAnswer: (String, AnswerPayload) -> Unit,
    onToggleKey: (String, String) -> Unit = { _, _ -> },
    onTextChange: (String, String) -> Unit = { _, _ -> },
    onToggleDoubtful: (String) -> Unit,
    onGoToIndex: (Int) -> Unit,
    onSubmit: () -> Unit,
    onRetry: () -> Unit,
    onFinish: () -> Unit,
    onSupervisorExit: () -> Unit = {},
    onSessionLost: (String) -> Unit = {},
    sessionId: String = "",
    exam: ExamSummaryDto? = null,
    baseUrl: String = "",
    imageLoader: coil3.ImageLoader? = null
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showQuestionList by remember { mutableStateOf(false) }
    var resumeNoticeDismissed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.isResumed) {
        if (state.isResumed) {
            kotlinx.coroutines.delay(8_000)
            resumeNoticeDismissed = true
        }
    }

    if (state.isSubmitted) {
        LaunchedEffect(Unit) {
            onFinish()
        }
    }

    LaunchedEffect(state.lostMessage) {
        state.lostMessage?.let(onSessionLost)
    }

    LaunchedEffect(state.submitError) {
        if (state.submitError != null && !state.isTimeOver && !state.violationExit) {
            showConfirmDialog = true
        }
    }

    if (state.violationExit && !state.isSubmitted) {
        AppDialog(
            title = "Ujian dikumpulkan",
            onDismiss = {},
            dismissible = false,
            footerEnd = if (!state.isSubmitting && state.submitError != null) {
                { ActionButton(text = "Coba lagi", onClick = onSubmit) }
            } else {
                null
            }
        ) {
            Text(
                text = state.submitError?.let { "${ViolationPolicy.warning(ViolationPolicy.LIMIT + 1)} $it Mencoba lagi." }
                    ?: "${ViolationPolicy.warning(ViolationPolicy.LIMIT + 1)} Jawaban dikirim seadanya.",
                fontFamily = DMSansFontFamily,
                fontSize = 13.sp,
                color = TextPrimary
            )
        }
    }

    if (state.isTimeOver && !state.isSubmitted) {
        AppDialog(
            title = "Waktu habis",
            onDismiss = {},
            dismissible = false,
            footerEnd = if (!state.isSubmitting && state.submitError != null) {
                { ActionButton(text = "Coba lagi", onClick = onSubmit) }
            } else {
                null
            }
        ) {
            Text(
                text = state.submitError?.let { "Mengirim jawaban… $it Mencoba lagi." }
                    ?: "Mengirim jawaban…",
                fontFamily = DMSansFontFamily,
                fontSize = 13.sp,
                color = TextPrimary
            )
        }
    }

    if (showConfirmDialog && !state.violationExit) {
        val summary = submitSummary(state)
        AppDialog(
            title = "Kumpulkan Lembar Ujian?",
            onDismiss = { if (!state.isSubmitting) showConfirmDialog = false },
            dismissible = !state.isSubmitting,
            footerStart = {
                ActionButton(
                    text = "Periksa Kembali",
                    style = ActionStyle.Outline,
                    enabled = !state.isSubmitting,
                    onClick = { showConfirmDialog = false }
                )
            },
            footerEnd = {
                ActionButton(
                    text = "Ya, Kumpulkan",
                    enabled = !state.isSubmitting,
                    onClick = {
                        showConfirmDialog = false
                        onSubmit()
                    }
                )
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SubmitCountRow("Dijawab", summary.answered, StatusSuccess)
                SubmitCountRow("Ragu-ragu", summary.doubtful, DoubtfulOrange)
                SubmitCountRow("Belum dijawab", summary.unanswered, StatusError)
                SubmitCountRow("Belum terkirim", summary.pendingSync, if (summary.pendingSync > 0) DoubtfulOrange else StatusSuccess)
                Text(
                    text = "Dari ${summary.total} soal. Setelah dikumpulkan, jawaban tidak dapat diubah kembali.",
                    fontFamily = DMSansFontFamily,
                    fontSize = 12.sp,
                    color = TextMuted
                )
                if (state.isSubmitting) {
                    Text(
                        text = "Mengirim jawaban…",
                        fontFamily = DMSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                state.submitError?.let { error ->
                    Text(
                        text = error,
                        fontFamily = DMSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = StatusError
                    )
                }
            }
        }
    }

    val answeredCount = state.questions.count { q ->
        state.answers[q.id].isFilled() || !state.drafts[q.id].isNullOrBlank()
    }

    if (showQuestionList) {
        QuestionListDialog(
            state = state,
            onGoToIndex = {
                showQuestionList = false
                onGoToIndex(it)
            },
            onDismiss = { showQuestionList = false }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandSurface)
                    .windowInsetsPadding(
                        WindowInsets.statusBars.union(WindowInsets.displayCutout)
                    )
                    .heightIn(min = 54.dp),
                color = BrandSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_name),
                            contentDescription = "Logo",
                            modifier = Modifier.height(28.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val minutes = state.remainingSeconds / 60
                        val seconds = state.remainingSeconds % 60
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BrandMuted,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Text(
                                text = "%02d:%02d".format(minutes, seconds),
                                fontFamily = DMSansFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (state.remainingSeconds < 300) StatusError else TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandSurface)
                    .windowInsetsPadding(
                        WindowInsets.navigationBars.union(WindowInsets.ime)
                    )
            ) {
            if (state.isResumed && !resumeNoticeDismissed) {
                ResumeNotice(onClose = { resumeNoticeDismissed = true })
            }
            if (state.questions.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    val currentQ = state.questions[state.currentIndex]
                    val isDoubt = state.doubtfuls.contains(currentQ.id)
                    val isLastQuestion = state.currentIndex >= state.questions.size - 1
                    val primaryLabel =
                        if (isLastQuestion) {
                            when {
                                state.isSubmitting -> "Mengirim…"
                                state.submitGateSeconds > 0L -> "Kumpulkan (${state.submitGateMinutes} mnt)"
                                else -> "Kumpulkan"
                            }
                        } else {
                            "Selanjutnya"
                        }
                    val primaryEnabled = if (isLastQuestion) state.canSubmit else !state.isSubmitting
                    val primaryColor = if (isLastQuestion) SubmitGreen else BrandNavy
                    val onPrimaryClick: () -> Unit =
                        if (isLastQuestion) {
                            { showConfirmDialog = true }
                        } else {
                            { onGoToIndex(state.currentIndex + 1) }
                        }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExamPreviousButton(
                            enabled = state.currentIndex > 0 && !state.isSubmitting,
                            onClick = { onGoToIndex(state.currentIndex - 1) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        )
                        ExamDoubtButton(
                            isDoubt = isDoubt,
                            enabled = state.canAnswer,
                            onClick = { onToggleDoubtful(currentQ.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        )
                        ExamPrimaryAction(
                            label = primaryLabel,
                            enabled = primaryEnabled,
                            containerColor = primaryColor,
                            onClick = onPrimaryClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        )
                    }
                }
            }
            }
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BrandBackground)
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandNavy)
                }
            }

            state.errorMessage != null -> {
                ExamErrorContent(
                    message = state.errorMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BrandBackground)
                        .padding(padding),
                    onRetry = onRetry,
                    onSupervisorExit = onSupervisorExit
                )
            }

            state.questions.isEmpty() -> {
                ExamErrorContent(
                    message = ExamViewModel.EMPTY_PAPER_MESSAGE,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BrandBackground)
                        .padding(padding),
                    onSupervisorExit = onSupervisorExit,
                    onRetry = onRetry
                )
            }

            else -> {
                val currentQ = state.questions[state.currentIndex]

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BrandBackground)
                        .padding(padding)
                ) {
                BrandWatermark(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 120.dp, y = 140.dp)
                        .width(380.dp)
                        .rotate(-24f)
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    state.deliveryWarning?.let { warning ->
                        Text(
                            text = warning,
                            fontFamily = DMSansFontFamily,
                            fontSize = 12.sp,
                            color = StatusError,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    QuestionDetailCard(
                        state = state,
                        question = currentQ,
                        answeredCount = answeredCount,
                        exam = exam,
                        onOpenList = { showQuestionList = true },
                        onSubmit = { showConfirmDialog = true }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        item {
                            SectionedCard {
                                SectionHeader("Soal ${state.currentIndex + 1}") {
                                    SyncIndicator(state.sync)
                                }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    HtmlContent(
                                        html = currentQ.bodyHtml,
                                        sessionId = sessionId,
                                        baseUrl = baseUrl,
                                        imageLoader = imageLoader
                                    )
                                }
                                SectionHeader("Jawaban", divider = true)
                                Column(modifier = Modifier.padding(16.dp)) {
                                    QuestionAnswerInput(
                                        question = currentQ,
                                        answer = state.answers[currentQ.id],
                                        draft = state.drafts[currentQ.id],
                                        enabled = state.canAnswer,
                                        images = ImageContext(sessionId, baseUrl, imageLoader),
                                        onAnswer = { onAnswer(currentQ.id, it) },
                                        onToggleKey = { onToggleKey(currentQ.id, it) },
                                        onTextChange = { onTextChange(currentQ.id, it) }
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun ExamErrorContent(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    onSupervisorExit: () -> Unit = {}
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = StatusError,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Soal gagal dimuat",
                fontFamily = DMSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontFamily = DMSansFontFamily,
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    text = "Coba Lagi",
                    fontFamily = DMSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Keluar darurat (pengawas)",
                fontFamily = DMSansFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onSupervisorExit)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ExamPreviousButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = modifier
    ) {
        Text(
            text = "Sebelumnya",
            fontFamily = DMSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExamDoubtButton(
    isDoubt: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDoubt) DoubtfulOrange else BorderSubtle
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isDoubt) DoubtfulOrangeLight else Color.Transparent
        ),
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = if (isDoubt) DoubtfulOrange else TextMuted,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Ragu-ragu",
            fontFamily = DMSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = if (isDoubt) DoubtfulOrange else TextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExamPrimaryAction(
    label: String,
    enabled: Boolean,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontFamily = DMSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            maxLines = 2,
            softWrap = true,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuestionGridGlyph() {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
        val cell = size.width * 0.38f
        val gap = size.width - cell * 2
        for (row in 0..1) {
            for (col in 0..1) {
                drawRoundRect(
                    color = BrandNavy,
                    topLeft = androidx.compose.ui.geometry.Offset(col * (cell + gap), row * (cell + gap)),
                    size = androidx.compose.ui.geometry.Size(cell, cell),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.25f)
                )
            }
        }
    }
}

@Composable
private fun QuestionListDialog(
    state: ExamUiState,
    onGoToIndex: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    fun answered(q: PaperQuestionDto) = state.answers[q.id].isFilled() || !state.drafts[q.id].isNullOrBlank()
    val answeredCount = state.questions.count(::answered)
    val doubtCount = state.questions.count { state.doubtfuls.contains(it.id) }
    val groups = state.questions.withIndex().groupBy { it.value.type }

    AppDialog(
        title = "Daftar Soal",
        onDismiss = onDismiss,
        footerContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(BrandNavy, "Sekarang")
                LegendDot(StatusSuccess, "Terjawab $answeredCount")
                LegendDot(DoubtfulOrange, "Ragu-ragu $doubtCount")
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            groups.forEach { (type, items) ->
                val shape = RoundedCornerShape(10.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandSurface, shape)
                        .border(1.dp, BorderSubtle, shape)
                        .clip(shape)
                ) {
                    CardHeader(title = questionTypeLabel(type)) {
                        Text(
                            text = "${items.count { answered(it.value) }} dari ${items.size}",
                            fontFamily = DMSansFontFamily,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items.chunked(10).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                rowItems.forEach { (index, q) ->
                                    val isCurrent = index == state.currentIndex
                                    val isAnswered = answered(q)
                                    val isDoubtItem = state.doubtfuls.contains(q.id)
                                    val bgColor = when {
                                        isCurrent -> BrandNavy
                                        isDoubtItem -> DoubtfulOrange
                                        isAnswered -> StatusSuccess
                                        else -> BrandSurface
                                    }
                                    val textColor =
                                        if (isCurrent || isDoubtItem || isAnswered) Color.White else TextSecondary
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(bgColor)
                                            .border(1.dp, if (isCurrent) BrandNavy else BorderSubtle, RoundedCornerShape(6.dp))
                                            .clickable { onGoToIndex(index) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            fontFamily = DMSansFontFamily,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            color = textColor
                                        )
                                    }
                                }
                                repeat(10 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontFamily = DMSansFontFamily,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun QuestionDetailCard(
    state: ExamUiState,
    question: PaperQuestionDto,
    answeredCount: Int,
    exam: ExamSummaryDto?,
    onOpenList: () -> Unit,
    onSubmit: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val isDoubt = state.doubtfuls.contains(question.id)
    val isAnswered = state.answers[question.id].isFilled() || !state.drafts[question.id].isNullOrBlank()
    val status = when {
        isDoubt -> "Ragu-ragu"
        isAnswered -> "Sudah dijawab"
        else -> "Belum dijawab"
    }
    val statusColor = when {
        isDoubt -> DoubtfulOrange
        isAnswered -> StatusSuccess
        else -> TextMuted
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandSurface, shape)
            .border(1.dp, BorderSubtle, shape)
            .clip(shape)
    ) {
        CardHeader(title = "Detail Soal") {
            Row(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandSurface)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenList)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuestionGridGlyph()
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Daftar Soal",
                    fontFamily = DMSansFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = BrandNavy
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            exam?.let { DetailRow("Ujian", it.displayTitle(), strong = true) }
            DetailRow(
                "Jenis soal",
                questionTypeLabel(question.type),
                strong = true
            )
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(
                    text = "Status",
                    fontFamily = DMSansFontFamily,
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.width(88.dp)
                )
                Text(
                    text = status,
                    fontFamily = DMSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = statusColor,
                    modifier = Modifier.weight(1f)
                )
            }
            DetailRow("Terjawab", "$answeredCount dari ${state.questions.size} soal")
            if (state.submitGateSeconds > 0L) {
                Text(
                    text = "Dapat dikumpulkan dalam ${state.submitGateMinutes} menit",
                    fontFamily = DMSansFontFamily,
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            ActionButton(
                text = if (state.isSubmitting) "Mengirim…" else "Selesai & Kumpulkan",
                onClick = onSubmit,
                style = ActionStyle.Outline,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ResumeNotice(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .background(DoubtfulOrangeLight, RoundedCornerShape(8.dp))
            .border(1.dp, DoubtfulOrange, RoundedCornerShape(8.dp))
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Ujian dilanjutkan. Sisa waktu mengikuti jam sebenarnya.",
            fontFamily = DMSansFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = DoubtfulOrange,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onClose)
                .semantics { contentDescription = "Tutup pemberitahuan" },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(12.dp)) {
                val stroke = 1.8.dp.toPx()
                drawLine(DoubtfulOrange, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, size.height), stroke, androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(DoubtfulOrange, androidx.compose.ui.geometry.Offset(size.width, 0f), androidx.compose.ui.geometry.Offset(0f, size.height), stroke, androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
    }
}
