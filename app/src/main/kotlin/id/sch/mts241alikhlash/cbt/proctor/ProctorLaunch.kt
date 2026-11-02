package id.sch.mts241alikhlash.cbt.proctor

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import id.sch.mts241alikhlash.cbt.kiosk.InternalIntentGuard

@Composable
fun RequestCameraOnLaunch(guard: InternalIntentGuard? = null) {
    if (!ProctorFlags.ENABLED) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        val held = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (!held) {
            guard?.markLaunchingInternalDialog()
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
}
