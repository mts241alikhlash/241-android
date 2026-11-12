package id.sch.mts241alikhlash.exam.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.exam.api.exam.models.MyExamDto
import id.sch.mts241alikhlash.exam.data.repository.StudentProfile
import id.sch.mts241alikhlash.exam.ui.components.CardHeader
import id.sch.mts241alikhlash.exam.ui.components.PrimaryButton
import id.sch.mts241alikhlash.exam.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.exam.ui.theme.WebTokens

fun profileCardExam(exams: List<MyExamDto>): MyExamDto? =
    // shortcut: ISO-8601 UTC strings sort lexicographically, parse if the formats ever vary
    exams.filter { it.lifecycle == MyExamDto.Lifecycle.OPEN }.minByOrNull { it.startsAt }

fun profileExamRows(exam: MyExamDto?): List<Pair<String, String>> {
    if (exam?.lifecycle != MyExamDto.Lifecycle.OPEN) return emptyList()
    val unset = "Belum ditetapkan"
    return listOfNotNull(
        exam.eventName?.takeIf { it.isNotBlank() }?.let { "Ujian" to it },
        "Ruangan" to (exam.roomName?.takeIf { it.isNotBlank() } ?: unset),
        exam.participantNumber?.takeIf { it.isNotBlank() }?.let { "No. peserta" to it },
        "No. meja" to (exam.seatNumber?.toString() ?: unset),
    )
}

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    exam: MyExamDto?,
) {
    val state by viewModel.state.collectAsState()
    when (val current = state) {
        ProfileUiState.Loading ->
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = WebTokens.Primary)
            }

        is ProfileUiState.Error ->
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Profil gagal dimuat",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = DMSansFontFamily,
                    color = WebTokens.Foreground,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = current.message,
                    fontSize = 13.sp,
                    fontFamily = DMSansFontFamily,
                    color = WebTokens.MutedForeground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                PrimaryButton(text = "Coba Lagi", onClick = viewModel::load, modifier = Modifier.width(160.dp))
            }

        is ProfileUiState.Content -> ProfileContent(current.profile, exam)
    }
}

@Composable
private fun ProfileContent(
    profile: StudentProfile,
    exam: MyExamDto?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val shape = RoundedCornerShape(10.dp)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(WebTokens.Card, shape)
                    .border(1.dp, WebTokens.Border, shape)
                    .clip(shape),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().height(64.dp).background(WebTokens.Primary))
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 28.dp)
                            .background(WebTokens.Card, CircleShape)
                            .padding(4.dp),
                ) {
                    Avatar(profile.initials, size = 72)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = profile.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DMSansFontFamily,
                color = WebTokens.Foreground,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(16.dp))
        }
        Section(title = "Ujian aktif", allRows = profileExamRows(exam))
        Section(
            title = "Data siswa",
            allRows =
                listOf(
                    "Nama lengkap" to profile.name,
                    "NIS" to profile.nis,
                    "NISN" to profile.nisn.ifBlank { null },
                    "Status" to profile.status,
                ),
        )
        Section(
            title = "Data akun",
            allRows =
                listOf(
                    "Username" to (profile.username ?: profile.nis),
                ),
        )
        Section(
            title = "Data kelas",
            allRows =
                listOf(
                    "Kelas" to profile.classLabel,
                    "Tingkat" to profile.gradeName,
                    "Semester" to profile.semester,
                    "Tahun ajaran" to profile.academicYear,
                ),
        )
        Section(
            title = "Data diri",
            allRows =
                listOf(
                    "Jenis kelamin" to profile.gender,
                    "Tempat lahir" to profile.birthPlace,
                    "Tanggal lahir" to profile.birthDate,
                ),
        )
        Section(
            title = "Kontak",
            allRows =
                listOf(
                    "Email" to profile.email,
                    "Telepon" to profile.phone,
                ),
        )
    }
}

@Composable
private fun Section(
    title: String,
    allRows: List<Pair<String, String?>>,
) {
    val rows = allRows.mapNotNull { (label, value) -> value?.let { label to it } }
    if (rows.isEmpty()) return
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(WebTokens.Card, shape)
                .border(1.dp, WebTokens.Border, shape)
                .clip(shape),
    ) {
        CardHeader(title)
        Box(Modifier.fillMaxWidth().height(1.dp).background(WebTokens.Border))
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            rows.forEach { (label, value) -> DetailRow(label, value) }
        }
    }
}

@Composable
fun Avatar(
    initials: String,
    size: Int,
) {
    Box(
        modifier =
            Modifier
                .size(size.dp)
                .background(WebTokens.Primary, CircleShape)
                .border(2.dp, WebTokens.BrandGold, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            fontSize = (size * 0.36f).sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = DMSansFontFamily,
            color = WebTokens.PrimaryForeground,
        )
    }
}
