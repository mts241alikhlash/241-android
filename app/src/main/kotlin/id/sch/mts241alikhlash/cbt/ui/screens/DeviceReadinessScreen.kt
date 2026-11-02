package id.sch.mts241alikhlash.cbt.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import id.sch.mts241alikhlash.cbt.kiosk.DeviceReadinessState
import id.sch.mts241alikhlash.cbt.kiosk.DndController
import id.sch.mts241alikhlash.cbt.kiosk.InternalIntentGuard
import id.sch.mts241alikhlash.cbt.proctor.CameraPermissionState
import id.sch.mts241alikhlash.cbt.proctor.ProctorFlags
import id.sch.mts241alikhlash.cbt.proctor.cameraPermissionState
import id.sch.mts241alikhlash.cbt.proctor.findActivity
import id.sch.mts241alikhlash.cbt.proctor.requestCameraAgain
import id.sch.mts241alikhlash.cbt.ui.components.PrimaryButton
import id.sch.mts241alikhlash.cbt.ui.components.rememberIsOnline
import id.sch.mts241alikhlash.cbt.ui.theme.BrandBackground
import id.sch.mts241alikhlash.cbt.ui.theme.BrandNavy
import id.sch.mts241alikhlash.cbt.ui.theme.BrandSurface
import id.sch.mts241alikhlash.cbt.ui.theme.BorderSubtle
import id.sch.mts241alikhlash.cbt.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.cbt.ui.theme.StatusError
import id.sch.mts241alikhlash.cbt.ui.theme.StatusSuccess
import id.sch.mts241alikhlash.cbt.ui.theme.TextMuted
import id.sch.mts241alikhlash.cbt.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun DeviceReadinessScreen(
    onReadyToProceed: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dnd = remember(context) { DndController(context) }
    val online = rememberIsOnline()
    var askedCamera by rememberSaveable { mutableStateOf(false) }
    var readiness by remember { mutableStateOf(DeviceReadinessState()) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val cameraRequired = ProctorFlags.ENABLED
    val refresh: () -> Unit = {
        readiness = DeviceReadinessState(
            dndGranted = dnd.isAccessGranted(),
            cameraGranted = !cameraRequired ||
                cameraPermissionState(context, askedCamera) == CameraPermissionState.GRANTED,
            audioClear = isAudioClear(context)
        )
    }
    val currentRefresh by rememberUpdatedState(refresh)

    LaunchedEffect(Unit) {
        while (true) {
            currentRefresh()
            delay(1_000)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val cameraState = cameraPermissionState(context, askedCamera)
    val state = readiness.copy(online = online)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Cek Kesiapan HP",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = DMSansFontFamily,
            letterSpacing = (-0.4).sp,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Lengkapi empat syarat berikut sebelum memasukkan token ujian.",
            fontSize = 13.sp,
            fontFamily = DMSansFontFamily,
            color = TextMuted
        )
        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandSurface, RoundedCornerShape(10.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
        ) {
            ReadinessRow(
                title = "Mode Jangan Ganggu",
                detail = if (state.dndGranted) {
                    "Notifikasi dan dering akan dibisukan selama ujian."
                } else {
                    "Izinkan akses Jangan Ganggu agar notifikasi tidak mengganggu ujian."
                },
                ok = state.dndGranted,
                actionLabel = if (state.dndGranted) null else "Aktifkan Izin Jangan Ganggu",
                onAction = { openDndSettings(context, dnd) }
            )
            RowDivider()
            ReadinessRow(
                title = "Izin Kamera Pengawas",
                detail = when {
                    !cameraRequired -> "Pengawasan kamera tidak aktif pada aplikasi ini."
                    cameraState == CameraPermissionState.GRANTED -> "Kamera siap digunakan pengawas."
                    cameraState == CameraPermissionState.BLOCKED -> "Izin kamera dimatikan. Buka Pengaturan, lalu izinkan Kamera."
                    else -> "Izinkan kamera sebelum ujian dimulai."
                },
                ok = state.cameraGranted,
                actionLabel = if (!cameraRequired || state.cameraGranted) null else {
                    if (cameraState == CameraPermissionState.BLOCKED) "Buka Pengaturan" else "Izinkan Kamera"
                },
                onAction = {
                    requestCameraAgain(context, cameraState) {
                        askedCamera = true
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            )
            RowDivider()
            ReadinessRow(
                title = "Bebas Panggilan & Mikrofon",
                detail = if (state.audioClear) {
                    "Tidak ada panggilan atau perekam suara yang aktif."
                } else {
                    "Matikan panggilan telepon atau perekam suara aplikasi lain terlebih dahulu."
                },
                ok = state.audioClear,
                actionLabel = "Periksa Ulang",
                onAction = refresh
            )
            RowDivider()
            ReadinessRow(
                title = "Koneksi Internet",
                detail = if (state.online) {
                    "Terhubung ke jaringan."
                } else {
                    "Sambungkan HP ke internet sebelum melanjutkan."
                },
                ok = state.online,
                actionLabel = "Periksa Ulang",
                onAction = refresh
            )
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = "Lanjutkan",
            onClick = onReadyToProceed,
            enabled = state.allReady
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Kembali ke daftar ujian",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSansFontFamily,
            color = BrandNavy,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
}

@Composable
private fun ReadinessRow(
    title: String,
    detail: String,
    ok: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (ok) StatusSuccess else StatusError)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DMSansFontFamily,
                color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                fontSize = 12.sp,
                fontFamily = DMSansFontFamily,
                color = TextMuted
            )
            if (actionLabel != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DMSansFontFamily,
                    color = BrandNavy,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onAction)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Text(
            text = if (ok) "Siap" else "Belum",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = DMSansFontFamily,
            color = if (ok) StatusSuccess else StatusError
        )
    }
}

private fun isAudioClear(context: Context): Boolean {
    val audio = context.getSystemService(AudioManager::class.java) ?: return true
    val recording = audio.activeRecordingConfigurations.isNotEmpty()
    return audio.mode == AudioManager.MODE_NORMAL && !recording
}

private fun openDndSettings(context: Context, dnd: DndController) {
    val activity = context.findActivity()
    if (activity != null) {
        dnd.openPolicySettings(activity, InternalIntentGuard.shared)
    } else {
        InternalIntentGuard.shared.markLaunchingInternalDialog()
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
