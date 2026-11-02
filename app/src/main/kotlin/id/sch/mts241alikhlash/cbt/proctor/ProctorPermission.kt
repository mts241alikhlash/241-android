package id.sch.mts241alikhlash.cbt.proctor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import id.sch.mts241alikhlash.cbt.ui.theme.WebTokens

enum class CameraPermissionState { GRANTED, ASK, DENIED, BLOCKED }

fun Context.findActivity(): android.app.Activity? {
    var current: Context? = this
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}

fun cameraPermissionState(context: Context, asked: Boolean): CameraPermissionState {
    val held = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED
    if (held) return CameraPermissionState.GRANTED
    if (!asked) return CameraPermissionState.DENIED
    val activity = context.findActivity() ?: return CameraPermissionState.DENIED
    return if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
        CameraPermissionState.DENIED
    } else {
        CameraPermissionState.BLOCKED
    }
}

@Composable
fun rememberProctorPermission(
    sessionId: String,
    controller: ProctorController,
): CameraPermissionState {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var state by remember { mutableStateOf(CameraPermissionState.ASK) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            state = CameraPermissionState.GRANTED
            controller.start(owner, sessionId)
        } else {
            val canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(
                context as? android.app.Activity ?: return@rememberLauncherForActivityResult,
                Manifest.permission.CAMERA,
            )
            state = if (canAskAgain) {
                CameraPermissionState.DENIED
            } else {
                CameraPermissionState.BLOCKED
            }
            controller.blocked(sessionId, "permission")
        }
    }

    LaunchedEffect(sessionId, controller) {
        if (!ProctorFlags.ENABLED) return@LaunchedEffect

        val held = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        state = if (held) {
            controller.start(owner, sessionId)
            CameraPermissionState.GRANTED
        } else {
            CameraPermissionState.DENIED
        }
    }

    return state
}

fun requestCameraAgain(
    context: android.content.Context,
    state: CameraPermissionState,
    launch: () -> Unit,
) {
    id.sch.mts241alikhlash.cbt.kiosk.InternalIntentGuard.shared.markLaunchingInternalDialog()
    if (state == CameraPermissionState.BLOCKED) {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } else {
        launch()
    }
}

@Composable
fun CameraPermissionBanner(
    state: CameraPermissionState,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state == CameraPermissionState.GRANTED || state == CameraPermissionState.ASK) return

    val blocked = state == CameraPermissionState.BLOCKED
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = WebTokens.Background),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Kamera belum diizinkan",
                fontSize = 13.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = WebTokens.Foreground,
            )
            Text(
                text = if (blocked) {
                    "Izin kamera dimatikan. Buka Pengaturan, lalu izinkan Kamera untuk aplikasi ini."
                } else {
                    "Ujian tetap berjalan, tetapi pengawas tidak bisa melihat kamera. Izinkan kamera sekarang."
                },
                fontSize = 12.sp,
                color = WebTokens.MutedForeground,
            )
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = WebTokens.Primary),
            ) {
                Text(if (blocked) "Buka Pengaturan" else "Izinkan kamera", fontSize = 13.sp)
            }
        }
    }
}
