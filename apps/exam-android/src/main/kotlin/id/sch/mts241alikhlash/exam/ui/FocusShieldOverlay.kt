package id.sch.mts241alikhlash.exam.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.exam.kiosk.ActiveInterference
import id.sch.mts241alikhlash.exam.ui.theme.DMSansFontFamily

@Composable
fun FocusShieldOverlay(
    modifier: Modifier = Modifier,
    interference: ActiveInterference? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text =
                when (interference) {
                    ActiveInterference.CALL_OR_MIC -> "Panggilan atau mikrofon aktif"
                    ActiveInterference.SCREEN_SHARE -> "Berbagi layar terdeteksi"
                    null -> "Layar ujian disembunyikan"
                },
            fontFamily = DMSansFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text =
                when (interference) {
                    ActiveInterference.CALL_OR_MIC ->
                        "Matikan panggilan atau perekaman suara untuk melanjutkan ujian."
                    ActiveInterference.SCREEN_SHARE ->
                        "Matikan berbagi layar atau proyeksi layar untuk melanjutkan ujian."
                    null -> "Soal akan muncul kembali saat layar ujian aktif."
                },
            fontFamily = DMSansFontFamily,
            fontSize = 13.sp,
            color = Color(0xFFB9BEC7),
            textAlign = TextAlign.Center,
        )
    }
}
