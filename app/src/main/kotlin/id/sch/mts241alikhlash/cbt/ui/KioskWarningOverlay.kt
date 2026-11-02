package id.sch.mts241alikhlash.cbt.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.cbt.ui.components.BrandWatermark
import id.sch.mts241alikhlash.cbt.ui.components.PrimaryButton
import id.sch.mts241alikhlash.cbt.ui.components.SectionHeader
import id.sch.mts241alikhlash.cbt.ui.components.SectionedCard
import id.sch.mts241alikhlash.cbt.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.cbt.ui.theme.WebTokens

@Composable
fun KioskWarningOverlay(
    onRetry: () -> Unit,
    onSupervisorAccess: () -> Unit = {},
    canProceedUnpinned: Boolean = false,
    onOpenPinSettings: () -> Unit = {},
    onProceedUnpinned: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize().background(WebTokens.Background)) {
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LockBadge(onLongPress = onSupervisorAccess)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Perangkat Belum Terkunci",
                fontFamily = DMSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                letterSpacing = (-0.4).sp,
                color = WebTokens.Foreground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Ujian dijeda karena kunci layar ditolak.",
                fontFamily = DMSansFontFamily,
                fontSize = 13.sp,
                color = WebTokens.MutedForeground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            SectionedCard {
                SectionHeader("YANG PERLU DILAKUKAN")
                Column(modifier = Modifier.padding(16.dp)) {
                    StepRow(1, "Panggil pengawas ujian.")
                    Spacer(Modifier.height(10.dp))
                    StepRow(2, "Ketuk \"Kunci Lagi\", lalu setujui kunci layar.")
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "* Jawaban yang sudah tersimpan tetap aman.",
                        fontFamily = DMSansFontFamily,
                        fontSize = 12.sp,
                        color = WebTokens.MutedForeground
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = "Kunci Lagi", onClick = onRetry)
            if (canProceedUnpinned) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Perangkat ini menolak kunci layar. Pilih salah satu:",
                    fontFamily = DMSansFontFamily,
                    fontSize = 12.sp,
                    color = WebTokens.MutedForeground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                PrimaryButton(text = "Buka Pengaturan Sematkan Layar", onClick = onOpenPinSettings)
                Spacer(Modifier.height(10.dp))
                PrimaryButton(text = "Lanjutkan dengan Mode Pengawasan Ketat", onClick = onProceedUnpinned)
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(WebTokens.Primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                fontFamily = DMSansFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = WebTokens.PrimaryForeground
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            fontFamily = DMSansFontFamily,
            fontSize = 13.sp,
            color = WebTokens.Foreground
        )
    }
}

@Composable
private fun LockBadge(onLongPress: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .background(WebTokens.Card, CircleShape)
            .border(1.dp, WebTokens.Border, CircleShape)
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = WebTokens.Destructive,
                topLeft = Offset(w * 0.12f, h * 0.45f),
                size = Size(w * 0.76f, h * 0.5f),
                cornerRadius = CornerRadius(w * 0.1f),
                style = stroke
            )
            drawArc(
                color = WebTokens.Destructive,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(w * 0.28f, h * 0.08f),
                size = Size(w * 0.44f, h * 0.7f),
                style = stroke
            )
            drawCircle(WebTokens.Destructive, radius = w * 0.06f, center = Offset(w * 0.5f, h * 0.7f))
        }
    }
}
