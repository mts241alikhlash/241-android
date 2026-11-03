package id.sch.mts241alikhlash.cbt.ui.screens

import androidx.compose.foundation.background
import id.sch.mts241alikhlash.cbt.ui.components.AppDialog
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.cbt.data.model.ExamSummaryDto
import id.sch.mts241alikhlash.cbt.ui.components.ActionButton
import id.sch.mts241alikhlash.cbt.ui.components.CardHeader
import id.sch.mts241alikhlash.cbt.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.cbt.ui.theme.WebTokens

private data class ScheduleEntry(val exam: ExamSummaryDto, val window: ExamWindow)

private data class ScheduleGroup(val title: String, val entries: List<ScheduleEntry>)

private val TYPE_ORDER = listOf("UTS", "UAS", "UH", "QUIZ", "ASSIGNMENT")

private fun groupsOf(entries: List<ScheduleEntry>): List<ScheduleGroup> {
    val known = TYPE_ORDER.mapNotNull { type ->
        val inType = entries.filter { it.exam.type == type }
        if (inType.isEmpty()) null else ScheduleGroup("JADWAL ${examTypeName(type).uppercase()}", inType)
    }
    val other = entries.filter { it.exam.type !in TYPE_ORDER }
    return if (other.isEmpty()) known else known + ScheduleGroup("JADWAL UJIAN LAINNYA", other)
}

private fun examTypeName(type: String): String = when (type) {
    "UH" -> "Ulangan Harian"
    "QUIZ" -> "Quiz"
    "ASSIGNMENT" -> "Tugas"
    else -> type
}

private fun scheduleOf(exams: List<ExamSummaryDto>): List<ScheduleEntry> =
    exams
        .filter { it.status != "FINISHED" }
        .sortedBy { it.startsAt }
        .mapNotNull { exam -> examWindow(exam.startsAt, exam.endsAt)?.let { ScheduleEntry(exam, it) } }

@Composable
fun ScheduleScreen(viewModel: ExamListViewModel) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.reload() }

    when (val current = state) {
        ExamListUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = WebTokens.Primary)
        }

        is ExamListUiState.Error -> Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Jadwal gagal dimuat",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DMSansFontFamily,
                color = WebTokens.Foreground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = current.message,
                fontSize = 13.sp,
                fontFamily = DMSansFontFamily,
                color = WebTokens.MutedForeground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            ActionButton(text = "Coba Lagi", onClick = viewModel::refresh)
        }

        ExamListUiState.Empty -> EmptySchedule()

        is ExamListUiState.Content -> {
            val groups = groupsOf(scheduleOf(current.exams))
            var selected by remember { mutableStateOf<ScheduleEntry?>(null) }
            if (groups.isEmpty()) {
                EmptySchedule()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(groups.size, key = { groups[it].title }) { index ->
                        ScheduleGroupCard(groups[index], onSelect = { selected = it })
                    }
                }
            }
            selected?.let { ScheduleDetailDialog(it) { selected = null } }
        }
    }
}

@Composable
private fun EmptySchedule() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Belum ada jadwal ujian",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = DMSansFontFamily,
            color = WebTokens.Foreground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Ujian yang dijadwalkan untuk kelas Anda akan tampil di sini.",
            fontSize = 13.sp,
            fontFamily = DMSansFontFamily,
            color = WebTokens.MutedForeground,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScheduleGroupCard(group: ScheduleGroup, onSelect: (ScheduleEntry) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WebTokens.Card, shape)
            .border(1.dp, WebTokens.Border, shape)
            .clip(shape)
    ) {
        CardHeader(group.title)
        group.entries.groupBy { it.window.date }.forEach { (date, entries) ->
            Box(Modifier.fillMaxWidth().height(1.dp).background(WebTokens.Border))
            Text(
                text = date,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DMSansFontFamily,
                color = WebTokens.Primary,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp)
            )
            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(entry) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.window.start,
                        fontSize = 13.sp,
                        fontFamily = DMSansFontFamily,
                        color = WebTokens.MutedForeground,
                        modifier = Modifier.width(52.dp)
                    )
                    Text(
                        text = entry.exam.subjectName?.takeIf { it.isNotBlank() } ?: entry.exam.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = DMSansFontFamily,
                        color = WebTokens.Foreground,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Canvas(modifier = Modifier.size(8.dp, 12.dp)) {
                        val stroke = 1.5.dp.toPx()
                        drawLine(WebTokens.MutedForeground, Offset(0f, 0f), Offset(size.width, size.height / 2), stroke, StrokeCap.Round)
                        drawLine(WebTokens.MutedForeground, Offset(size.width, size.height / 2), Offset(0f, size.height), stroke, StrokeCap.Round)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleDetailDialog(entry: ScheduleEntry, onDismiss: () -> Unit) {
    AppDialog(
        title = entry.exam.displayTitle(),
        onDismiss = onDismiss
    ) {
        if (entry.exam.subjectName != null) DetailRow("Paket", entry.exam.title)
        DetailRow("Tanggal", entry.window.date)
        DetailRow("Mulai", entry.window.start)
        DetailRow("Selesai", entry.window.end)
        DetailRow("Durasi", "${entry.exam.durationMinutes} menit")
    }
}
