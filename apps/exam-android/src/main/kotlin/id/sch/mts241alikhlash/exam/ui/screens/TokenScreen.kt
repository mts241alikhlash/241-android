package id.sch.mts241alikhlash.exam.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.exam.R
import id.sch.mts241alikhlash.exam.api.exam.models.MyExamDto
import id.sch.mts241alikhlash.exam.ui.components.ActionButton
import id.sch.mts241alikhlash.exam.ui.components.ActionStyle
import id.sch.mts241alikhlash.exam.ui.components.BrandCardOverlap
import id.sch.mts241alikhlash.exam.ui.components.BrandEmblem
import id.sch.mts241alikhlash.exam.ui.components.BrandEmblemSize
import id.sch.mts241alikhlash.exam.ui.components.BrandWatermark
import id.sch.mts241alikhlash.exam.ui.components.CardHeader
import id.sch.mts241alikhlash.exam.ui.components.FloatingTextField
import id.sch.mts241alikhlash.exam.ui.components.PrimaryButton
import id.sch.mts241alikhlash.exam.ui.components.pullUp
import id.sch.mts241alikhlash.exam.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.exam.ui.theme.WebTokens

@Composable
private fun TokenHero(
    exam: MyExamDto?,
    resumeMode: Boolean,
    height: Dp,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height),
        contentAlignment = Alignment.TopCenter,
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                WebTokens.Primary.copy(alpha = 0.82f),
                                WebTokens.Primary.copy(alpha = 0.96f),
                            ),
                        ),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, WebTokens.Background))),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = BrandCardOverlap + BrandEmblemSize / 2),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (resumeMode) "Lanjutkan ujian" else "Ujian digital",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSansFontFamily,
                letterSpacing = 1.8.sp,
                color = WebTokens.PrimaryForeground.copy(alpha = 0.82f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = exam?.displayTitle() ?: "Masukkan token ujian",
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DMSansFontFamily,
                color = WebTokens.PrimaryForeground,
                modifier = Modifier.padding(horizontal = 28.dp),
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier =
                    Modifier
                        .width(28.dp)
                        .height(2.dp)
                        .background(WebTokens.BrandGold),
            )
        }
    }
}

@Composable
fun TokenScreen(
    onStartExam: (sessionId: String) -> Unit,
    exam: MyExamDto? = null,
    loading: Boolean = false,
    error: String? = null,
    onBack: (() -> Unit)? = null,
    resumeMode: Boolean = false,
    onSupervisorExit: (() -> Unit)? = null,
) {
    var token by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            errorMessage = "Token ujian wajib diisi"
        } else {
            errorMessage = null
            onStartExam(trimmed)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(WebTokens.Background)) {
        val screenHeight = maxHeight
        val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val scrollState = rememberScrollState()
        LaunchedEffect(imeVisible) {
            if (!imeVisible) scrollState.scrollTo(0)
        }
        BrandWatermark(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = maxWidth * 0.28f, y = 110.dp)
                    .width(maxWidth * 1.1f)
                    .rotate(-24f),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.statusBars
                            .union(WindowInsets.displayCutout)
                            .union(WindowInsets.navigationBars)
                            .union(WindowInsets.ime),
                    ).then(if (imeVisible) Modifier.verticalScroll(scrollState) else Modifier)
                    .heightIn(min = screenHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TokenHero(
                    exam = exam,
                    resumeMode = resumeMode,
                    height = (screenHeight * 0.32f).coerceAtLeast(200.dp),
                )
                Column(
                    modifier =
                        Modifier
                            .pullUp(BrandCardOverlap)
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    val radius = CornerRadius((WebTokens.Radius + 2.dp).toPx())
                                    drawRoundRect(WebTokens.Card, cornerRadius = radius)
                                    drawRoundRect(WebTokens.Border, cornerRadius = radius, style = Stroke(1.dp.toPx()))
                                },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BrandEmblem()
                        Column(
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = if (resumeMode) "Masukkan Token Kembali" else "Token Ujian",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = DMSansFontFamily,
                                letterSpacing = (-0.4).sp,
                                color = WebTokens.Foreground,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text =
                                    if (resumeMode) {
                                        "Anda keluar dari aplikasi. Minta token terbaru kepada pengawas ruangan untuk melanjutkan."
                                    } else {
                                        "Minta token kepada pengawas ruangan, lalu masukkan di bawah ini."
                                    },
                                fontSize = 13.sp,
                                fontFamily = DMSansFontFamily,
                                color = WebTokens.MutedForeground,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(12.dp))
                            FloatingTextField(
                                value = token,
                                onValueChange = {
                                    token = it
                                    if (errorMessage != null) errorMessage = null
                                },
                                label = "Token ujian",
                                error = errorMessage ?: error,
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions =
                                    KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters,
                                        imeAction = ImeAction.Go,
                                    ),
                                keyboardActions = KeyboardActions(onGo = { submit() }),
                            )
                            Spacer(Modifier.height(18.dp))
                            PrimaryButton(
                                text = if (resumeMode) "Lanjutkan Ujian" else "Mulai Ujian",
                                onClick = { submit() },
                                enabled = !loading,
                                loading = loading,
                            )
                        }
                    }
                    if (exam != null) {
                        Spacer(Modifier.height(16.dp))
                        ExamDetailCard(exam)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (onBack != null && !resumeMode) {
                        Text(
                            text = "Kembali ke daftar ujian",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = DMSansFontFamily,
                            color = if (loading) WebTokens.MutedForeground else WebTokens.Primary,
                            modifier =
                                Modifier
                                    .clickable(enabled = !loading, onClick = onBack)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    if (onSupervisorExit != null) {
                        ActionButton(
                            text = "Keluar darurat (pengawas)",
                            style = ActionStyle.Outline,
                            enabled = !loading,
                            onClick = onSupervisorExit,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        text = "Layar dikunci selama ujian berlangsung.",
                        fontSize = 12.sp,
                        fontFamily = DMSansFontFamily,
                        color = WebTokens.MutedForeground,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Istiqra · MTs Persis 241 Al-Ikhlash",
                    fontSize = 11.sp,
                    fontFamily = DMSansFontFamily,
                    color = WebTokens.MutedForeground,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ExamDetailCard(exam: MyExamDto) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(WebTokens.Card, shape)
                .border(1.dp, WebTokens.Border, shape)
                .clip(shape),
    ) {
        CardHeader("Detail Ujian")
        Box(Modifier.fillMaxWidth().height(1.dp).background(WebTokens.Border))
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            DetailRow("Ujian", exam.displayTitle(), strong = true)
            if (exam.displayTitle() != exam.title) DetailRow("Paket", exam.title)
            examWindow(exam.startsAt, exam.endsAt)?.let {
                DetailRow("Tanggal", it.date)
                DetailRow("Mulai", it.start)
                DetailRow("Selesai", it.end)
            }
            DetailRow("Durasi", "${exam.durationMinutes} menit")
            exam.participantNumber?.let { DetailRow("No. peserta", it, strong = true) }
        }
    }
}
