package id.sch.mts241alikhlash.istiqra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sch.mts241alikhlash.istiqra.api.exam.models.MyExamDto
import id.sch.mts241alikhlash.istiqra.data.auth.AuthUser
import id.sch.mts241alikhlash.istiqra.ui.screens.DeviceReadinessScreen
import id.sch.mts241alikhlash.istiqra.ui.screens.TokenScreen
import id.sch.mts241alikhlash.istiqra.ui.theme.AssessmentTheme
import id.sch.mts241alikhlash.istiqra.ui.theme.BrandBackground
import id.sch.mts241alikhlash.istiqra.ui.theme.BrandNavy
import id.sch.mts241alikhlash.istiqra.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.istiqra.ui.theme.TextMuted
import id.sch.mts241alikhlash.istiqra.ui.theme.TextPrimary

data class AppRootState(
    val restoreComplete: Boolean = false,
    val restoreError: String? = null,
    val sessionId: String? = null,
    val deadline: Long? = null,
    val reentrySessionId: String? = null,
    val reentryLoading: Boolean = false,
    val reentryError: String? = null,
    val user: AuthUser? = null,
    val selectedExam: MyExamDto? = null,
    val readinessComplete: Boolean = false,
    val activeExam: MyExamDto? = null,
    val starting: Boolean = false,
    val startError: String? = null,
    val notice: String? = null,
) {
    fun afterRelease(notice: String? = null): AppRootState =
        copy(
            restoreError = null,
            sessionId = null,
            deadline = null,
            reentrySessionId = null,
            reentryLoading = false,
            reentryError = null,
            selectedExam = null,
            readinessComplete = false,
            activeExam = null,
            starting = false,
            startError = null,
            notice = notice,
        )
}

@Composable
fun AssessmentAppRoot(
    state: AppRootState,
    onRetryRestore: () -> Unit,
    onStartExam: (String) -> Unit,
    examContent: @Composable (sessionId: String, deadline: Long?) -> Unit,
    loginContent: @Composable () -> Unit = {},
    examListContent: @Composable () -> Unit = {},
    onBackToList: () -> Unit = {},
    onResumeWithToken: (String) -> Unit = {},
    onReadinessConfirmed: () -> Unit = {},
    onSupervisorExit: () -> Unit = {},
    readinessContent: @Composable (onReadyToProceed: () -> Unit) -> Unit = { ready ->
        DeviceReadinessScreen(onReadyToProceed = ready, onBack = onBackToList)
    },
    tokenContent: @Composable (MyExamDto) -> Unit = { exam ->
        TokenScreen(
            exam = exam,
            loading = state.starting,
            error = state.startError,
            onBack = onBackToList,
            onStartExam = onStartExam,
        )
    },
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    BackHandler(enabled = state.sessionId != null) {
        android.widget.Toast
            .makeText(
                context,
                "Tombol kembali dinonaktifkan selama ujian berlangsung.",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
    }

    BackHandler(enabled = state.sessionId == null && state.user != null && state.selectedExam != null) {
        if (!state.starting) onBackToList()
    }

    AssessmentTheme {
        when {
            !state.restoreComplete -> LoadingScreen()

            state.restoreError != null ->
                RestoreErrorScreen(
                    message = state.restoreError,
                    onRetry = onRetryRestore,
                )

            state.reentrySessionId != null ->
                TokenScreen(
                    onStartExam = onResumeWithToken,
                    loading = state.reentryLoading,
                    error = state.reentryError,
                    resumeMode = true,
                    onSupervisorExit = onSupervisorExit,
                )

            state.sessionId != null -> examContent(state.sessionId, state.deadline)

            state.user == null -> loginContent()

            state.selectedExam == null -> examListContent()

            !state.readinessComplete -> readinessContent(onReadinessConfirmed)

            else -> tokenContent(state.selectedExam)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BrandBackground),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = BrandNavy)
    }
}

@Composable
private fun RestoreErrorScreen(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BrandBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Sesi ujian gagal dipulihkan",
                fontFamily = DMSansFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontFamily = DMSansFontFamily,
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
            ) {
                Text(
                    text = "Coba Lagi",
                    fontFamily = DMSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
