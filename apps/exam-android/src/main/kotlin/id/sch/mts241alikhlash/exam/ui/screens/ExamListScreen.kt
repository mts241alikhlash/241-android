package id.sch.mts241alikhlash.exam.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.exam.data.model.ExamSummaryDto
import id.sch.mts241alikhlash.exam.ui.components.CardHeader
import id.sch.mts241alikhlash.exam.ui.components.PrimaryButton
import id.sch.mts241alikhlash.exam.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.exam.ui.theme.WebTokens

private data class ExamGroup(
    val title: String,
    val exams: List<ExamSummaryDto>,
)

private fun groupExams(exams: List<ExamSummaryDto>): List<ExamGroup> =
    listOf(
        ExamGroup("Bisa Dikerjakan", exams.filter { it.isSelectable() }),
        ExamGroup("Akan Datang", exams.filter { it.status == "UPCOMING" }),
        ExamGroup("Selesai", exams.filter { it.status == "FINISHED" }),
    ).filter { it.exams.isNotEmpty() }

@Composable
fun ExamListScreen(
    viewModel: ExamListViewModel,
    onSelect: (ExamSummaryDto) -> Unit,
    examWork: Boolean,
    notice: String? = null,
) {
    val state by viewModel.state.collectAsState()
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { viewModel.reload() }
    val emptyTitle = if (examWork) "Belum ada ujian" else "Belum ada non-ujian"
    val emptyBody =
        if (examWork) {
            "Tidak ada ujian untuk Anda saat ini."
        } else {
            "Tidak ada tugas non-ujian untuk Anda saat ini."
        }

    Column(modifier = Modifier.fillMaxSize()) {
        notice?.let {
            Text(
                text = it,
                fontSize = 12.sp,
                fontFamily = DMSansFontFamily,
                color = WebTokens.Foreground,
                modifier =
                    Modifier
                        .padding(start = 16.dp, top = 12.dp, end = 16.dp)
                        .fillMaxWidth()
                        .background(WebTokens.Card, RoundedCornerShape(8.dp))
                        .border(1.dp, WebTokens.Border, RoundedCornerShape(8.dp))
                        .padding(12.dp),
            )
        }
        when (val current = state) {
            ExamListUiState.Loading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = WebTokens.Primary)
                }

            ExamListUiState.Empty ->
                Message(
                    title = emptyTitle,
                    body = emptyBody,
                    actionLabel = "Muat Ulang",
                    onAction = viewModel::refresh,
                )

            is ExamListUiState.Error ->
                Message(
                    title = "Daftar ujian gagal dimuat",
                    body = current.message,
                    actionLabel = "Coba Lagi",
                    onAction = viewModel::refresh,
                )

            is ExamListUiState.Content -> {
                val exams = examMenuExams(current.exams, examWork)
                if (exams.isEmpty()) {
                    Message(
                        title = emptyTitle,
                        body = emptyBody,
                        actionLabel = "Muat Ulang",
                        onAction = viewModel::refresh,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            androidx.compose.foundation.layout
                                .PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        current.staleMessage?.let { message ->
                            item {
                                Text(
                                    text = "$message Menampilkan daftar terakhir.",
                                    fontSize = 12.sp,
                                    fontFamily = DMSansFontFamily,
                                    color = WebTokens.MutedForeground,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .background(WebTokens.Card, RoundedCornerShape(8.dp))
                                            .border(1.dp, WebTokens.Border, RoundedCornerShape(8.dp))
                                            .padding(12.dp),
                                )
                            }
                        }
                        groupExams(exams).forEach { group ->
                            item(key = "group-${group.title}") {
                                GroupCard(
                                    group = group,
                                    expandedId = expandedId,
                                    onToggle = { id -> expandedId = if (expandedId == id) null else id },
                                    onStart = onSelect,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: ExamGroup,
    expandedId: String?,
    onToggle: (String) -> Unit,
    onStart: (ExamSummaryDto) -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(WebTokens.Card, shape)
                .border(1.dp, WebTokens.Border, shape)
                .clip(shape),
    ) {
        CardHeader(group.title)
        group.exams.forEach { exam ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(WebTokens.Border))
            ExamRow(
                exam = exam,
                expanded = expandedId == exam.id,
                onToggle = { onToggle(exam.id) },
                onStart = onStart,
            )
        }
    }
}

@Composable
private fun ExamRow(
    exam: ExamSummaryDto,
    expanded: Boolean,
    onToggle: () -> Unit,
    onStart: (ExamSummaryDto) -> Unit,
) {
    val selectable = exam.isSelectable()
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    val expandable = exam.status != "FINISHED"
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = expandable, onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = exam.displayTitle(),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DMSansFontFamily,
                color = if (selectable) WebTokens.Foreground else WebTokens.MutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            if (!expandable) {
                examWindow(exam.startsAt, exam.endsAt)?.let {
                    Text(
                        text = it.shortDate,
                        fontSize = 12.sp,
                        fontFamily = DMSansFontFamily,
                        color = WebTokens.MutedForeground,
                    )
                }
            } else {
                Canvas(Modifier.size(12.dp).rotate(rotation)) {
                    val stroke = 1.8.dp.toPx()
                    drawLine(
                        WebTokens.MutedForeground,
                        Offset(size.width * 0.1f, size.height * 0.3f),
                        Offset(size.width * 0.5f, size.height * 0.7f),
                        stroke,
                        StrokeCap.Round,
                    )
                    drawLine(
                        WebTokens.MutedForeground,
                        Offset(size.width * 0.5f, size.height * 0.7f),
                        Offset(size.width * 0.9f, size.height * 0.3f),
                        stroke,
                        StrokeCap.Round,
                    )
                }
            }
        }
        if (expanded && expandable) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                StatusRow(exam)
                if (exam.displayTitle() != exam.title) DetailRow("Paket", exam.title)
                examWindow(exam.startsAt, exam.endsAt)?.let {
                    DetailRow("Tanggal", it.date)
                    DetailRow("Mulai", it.start)
                    DetailRow("Selesai", it.end)
                }
                DetailRow("Durasi", "${exam.durationMinutes} menit")
                exam.participantNumber?.let { DetailRow("No. peserta", it, strong = true) }
                run {
                    Spacer(Modifier.height(12.dp))
                    PrimaryButton(
                        text =
                            when (exam.status) {
                                "IN_PROGRESS" -> "Lanjutkan Ujian"
                                "UPCOMING" ->
                                    examOpensIn(exam.startsAt, System.currentTimeMillis())
                                        ?.let { "Dibuka dalam $it" } ?: "Belum Dibuka"
                                else -> "Mulai Ujian"
                            },
                        enabled = selectable,
                        onClick = { onStart(exam) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(exam: ExamSummaryDto) {
    val gold = Color(0xFF8A6D1C)
    val (text, color) =
        when (exam.status) {
            "OPEN" -> "Sedang dibuka" to WebTokens.Primary
            "IN_PROGRESS" -> "Sedang berlangsung" to gold
            "UPCOMING" -> "Belum dibuka" to gold
            else -> "Selesai" to WebTokens.MutedForeground
        }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = "Status",
            fontSize = 12.sp,
            fontFamily = DMSansFontFamily,
            color = WebTokens.MutedForeground,
            modifier = Modifier.width(88.dp),
        )
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = DMSansFontFamily,
            color = color,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun DetailRow(
    label: String,
    value: String,
    strong: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = DMSansFontFamily,
            color = WebTokens.MutedForeground,
            modifier = Modifier.width(88.dp),
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal,
            fontFamily = DMSansFontFamily,
            letterSpacing = if (strong) 0.5.sp else 0.sp,
            color = WebTokens.Foreground,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Message(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = DMSansFontFamily,
            color = WebTokens.Foreground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            fontSize = 13.sp,
            fontFamily = DMSansFontFamily,
            color = WebTokens.MutedForeground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        PrimaryButton(text = actionLabel, onClick = onAction, modifier = Modifier.width(160.dp))
    }
}
