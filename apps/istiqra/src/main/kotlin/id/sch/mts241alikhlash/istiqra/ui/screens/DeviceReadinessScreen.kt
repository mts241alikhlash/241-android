package id.sch.mts241alikhlash.istiqra.ui.screens

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import id.sch.mts241alikhlash.istiqra.kiosk.DeviceReadinessState
import id.sch.mts241alikhlash.istiqra.kiosk.DndController
import id.sch.mts241alikhlash.istiqra.kiosk.InternalIntentGuard
import id.sch.mts241alikhlash.istiqra.kiosk.isCurrentKeyboardTrusted
import id.sch.mts241alikhlash.istiqra.kiosk.showKeyboardPicker
import id.sch.mts241alikhlash.istiqra.ui.components.ActionButton
import id.sch.mts241alikhlash.istiqra.ui.components.ActionStyle
import id.sch.mts241alikhlash.istiqra.ui.components.CardHeader
import id.sch.mts241alikhlash.istiqra.ui.components.PrimaryButton
import id.sch.mts241alikhlash.istiqra.ui.components.findActivity
import id.sch.mts241alikhlash.istiqra.ui.components.rememberIsOnline
import id.sch.mts241alikhlash.istiqra.ui.theme.BorderSubtle
import id.sch.mts241alikhlash.istiqra.ui.theme.BrandBackground
import id.sch.mts241alikhlash.istiqra.ui.theme.BrandSurface
import id.sch.mts241alikhlash.istiqra.ui.theme.DMSansFontFamily
import id.sch.mts241alikhlash.istiqra.ui.theme.DoubtfulOrange
import id.sch.mts241alikhlash.istiqra.ui.theme.StatusError
import id.sch.mts241alikhlash.istiqra.ui.theme.StatusSuccess
import id.sch.mts241alikhlash.istiqra.ui.theme.TextMuted
import id.sch.mts241alikhlash.istiqra.ui.theme.TextPrimary
import kotlinx.coroutines.delay

@Composable
fun DeviceReadinessScreen(
    onReadyToProceed: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val dnd = remember(context) { DndController(context) }
    val online = rememberIsOnline()
    var readiness by remember { mutableStateOf(DeviceReadinessState()) }

    val refresh: () -> Unit = {
        readiness =
            DeviceReadinessState(
                dndGranted = dnd.isAccessGranted(),
                audioClear = isAudioClear(context),
                keyboardTrusted = isCurrentKeyboardTrusted(context),
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
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) currentRefresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val state = readiness.copy(online = online)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BrandBackground)
                .windowInsetsPadding(
                    WindowInsets.statusBars
                        .union(WindowInsets.displayCutout)
                        .union(WindowInsets.navigationBars)
                        .union(WindowInsets.ime),
                ).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        val readyCount =
            listOf(state.dndGranted, state.audioClear, state.online, state.keyboardTrusted)
                .count { it }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BrandSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
        ) {
            CardHeader("Izin Perangkat")
            RowDivider()
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(BorderSubtle),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(readyCount / READINESS_CHECKS)
                                .height(3.dp)
                                .background(if (readyCount == READINESS_CHECKS.toInt()) StatusSuccess else DoubtfulOrange),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "$readyCount/${READINESS_CHECKS.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = DMSansFontFamily,
                    color = if (readyCount == READINESS_CHECKS.toInt()) StatusSuccess else DoubtfulOrange,
                )
            }
            ReadinessRow(
                title = "Mode Jangan Ganggu",
                ok = state.dndGranted,
                actionLabel = if (state.dndGranted) null else "Izinkan",
                onAction = { openDndSettings(context, dnd) },
            )
            RowDivider()
            ReadinessRow(
                title = "Bebas Panggilan & Mikrofon",
                ok = state.audioClear,
                actionLabel = if (state.audioClear) null else "Periksa Ulang",
                onAction = refresh,
            )
            RowDivider()
            ReadinessRow(
                title = "Keyboard Bawaan HP",
                ok = state.keyboardTrusted,
                actionLabel = if (state.keyboardTrusted) null else "Ganti Keyboard",
                onAction = { showKeyboardPicker(context) },
            )
            RowDivider()
            ReadinessRow(
                title = "Koneksi Internet",
                ok = state.online,
                actionLabel = if (state.online) null else "Periksa Ulang",
                onAction = refresh,
            )
            RowDivider()
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                PrimaryButton(
                    text = "Lanjutkan",
                    onClick = onReadyToProceed,
                    enabled = state.allReady,
                )
                Spacer(Modifier.height(8.dp))
                ActionButton(
                    text = "Kembali ke daftar ujian",
                    onClick = onBack,
                    style = ActionStyle.Outline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))
}

@Composable
private fun ReadinessRow(
    title: String,
    ok: Boolean,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (actionLabel != null) Modifier.clickable(onClick = onAction) else Modifier,
                ).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (ok) StatusSuccess else StatusError.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (ok) "\u2713" else "!",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = DMSansFontFamily,
                color = if (ok) BrandSurface else StatusError,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSansFontFamily,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "\u203A",
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = DMSansFontFamily,
                color = TextMuted,
            )
        }
    }
}

private fun isAudioClear(context: Context): Boolean {
    val audio = context.getSystemService(AudioManager::class.java) ?: return true
    val recording = audio.activeRecordingConfigurations.isNotEmpty()
    return audio.mode == AudioManager.MODE_NORMAL && !recording
}

private fun openDndSettings(
    context: Context,
    dnd: DndController,
) {
    val activity = context.findActivity()
    if (activity != null) {
        dnd.openPolicySettings(activity, InternalIntentGuard.shared)
    } else {
        InternalIntentGuard.shared.markLaunchingInternalDialog()
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

private const val READINESS_CHECKS = 4f
