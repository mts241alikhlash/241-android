package id.sch.mts241alikhlash.cbt.kiosk

import id.sch.mts241alikhlash.cbt.data.local.CachedSessionEventEntity
import id.sch.mts241alikhlash.cbt.data.local.SessionEventDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class SessionEventType {
    PERMISSION_DIALOG,
    SCREEN_OFF,
    FOCUS_LOST_BRIEF,
    OVERLAY_OR_SHADE,
    CALL_OR_MIC_ACTIVE,
    SCREEN_SHARE_DETECTED,
    LEFT_APP,
    VIOLATION_LOCKED,
    KIOSK_MODE_CHANGED
}

data class RecordedSessionEvent(
    val clientId: String,
    val type: SessionEventType,
    val isViolation: Boolean,
    val occurredAtMs: Long,
    val detail: String?
)

class SessionEventTracker(
    private val dao: SessionEventDao,
    private val sessionId: String,
    private val scope: CoroutineScope,
    private val guard: InternalIntentGuard,
    private val clientId: () -> String = { UUID.randomUUID().toString() },
    private val nowMs: () -> Long = System::currentTimeMillis,
    writeDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val writeScope = CoroutineScope(SupervisorJob() + writeDispatcher)

    private val _requiresTokenReentry = MutableStateFlow(false)
    val requiresTokenReentry: StateFlow<Boolean> = _requiresTokenReentry.asStateFlow()

    private val _violationCount = MutableStateFlow(0)
    val violationCount: StateFlow<Int> = _violationCount.asStateFlow()

    private var focusLostAtMs: Long? = null
    private var focusLossJob: Job? = null
    private var overlayRecordedForFocusLoss = false
    private var callOrMicActive = false
    private var secondaryDisplayActive = false

    fun onWindowFocusChanged(hasFocus: Boolean, nowMs: Long) {
        if (hasFocus) {
            val lostAtMs = focusLostAtMs ?: return
            val job = focusLossJob
            val alreadyRecorded = overlayRecordedForFocusLoss
            val watchdogPending = job?.isActive == true
            clearFocusLoss()
            if (alreadyRecorded || !watchdogPending) return
            val elapsed = nowMs - lostAtMs
            if (elapsed < FOCUS_THRESHOLD_MS) {
                record(SessionEventType.FOCUS_LOST_BRIEF, false, nowMs, DETAIL_FOCUS_LOST_BRIEF)
            } else {
                record(SessionEventType.OVERLAY_OR_SHADE, true, nowMs, DETAIL_OVERLAY_OR_SHADE)
            }
        } else {
            if (focusLostAtMs != null) return
            focusLostAtMs = nowMs
            overlayRecordedForFocusLoss = false
            focusLossJob = scope.launch {
                delay(FOCUS_THRESHOLD_MS)
                if (focusLostAtMs == null) return@launch
                overlayRecordedForFocusLoss = true
                focusLossJob = null
                record(
                    SessionEventType.OVERLAY_OR_SHADE,
                    true,
                    nowMs(),
                    DETAIL_OVERLAY_OR_SHADE
                )
            }
        }
    }

    fun onActivityStopped(isScreenInteractive: Boolean, nowMs: Long) {
        val overlayAlreadyCounted = overlayRecordedForFocusLoss
        clearFocusLoss()
        when {
            guard.isGuarded ->
                record(SessionEventType.PERMISSION_DIALOG, false, nowMs, DETAIL_PERMISSION_DIALOG)
            !isScreenInteractive ->
                record(SessionEventType.SCREEN_OFF, false, nowMs, DETAIL_SCREEN_OFF)
            overlayAlreadyCounted -> Unit
            else -> {
                _requiresTokenReentry.value = true
                record(SessionEventType.LEFT_APP, true, nowMs, DETAIL_LEFT_APP)
            }
        }
    }

    fun onCallOrMicChanged(active: Boolean, nowMs: Long) {
        if (active == callOrMicActive) return
        callOrMicActive = active
        if (active) record(SessionEventType.CALL_OR_MIC_ACTIVE, false, nowMs, DETAIL_CALL_OR_MIC)
    }

    fun onSecondaryDisplayChanged(active: Boolean, nowMs: Long) {
        if (active == secondaryDisplayActive) return
        secondaryDisplayActive = active
        if (active) record(SessionEventType.SCREEN_SHARE_DETECTED, true, nowMs, DETAIL_SCREEN_SHARE)
    }

    fun recordKioskMode(mode: String, nowMs: Long) {
        record(SessionEventType.KIOSK_MODE_CHANGED, false, nowMs, mode)
    }

    fun close() {
        clearFocusLoss()
        writeScope.cancel()
    }

    private fun clearFocusLoss() {
        focusLostAtMs = null
        overlayRecordedForFocusLoss = false
        focusLossJob?.cancel()
        focusLossJob = null
    }

    private fun record(type: SessionEventType, isViolation: Boolean, nowMs: Long, detail: String) {
        val event = RecordedSessionEvent(
            clientId = clientId(),
            type = type,
            isViolation = isViolation,
            occurredAtMs = nowMs,
            detail = detail
        )
        writeScope.launch { dao.insert(event.toEntity()) }
        if (isViolation) _violationCount.value += 1
    }

    private fun RecordedSessionEvent.toEntity() = CachedSessionEventEntity(
        clientId = clientId,
        sessionId = sessionId,
        type = type.name,
        isViolation = isViolation,
        occurredAtMs = occurredAtMs,
        detail = detail
    )

    companion object {
        const val FOCUS_THRESHOLD_MS = 3_000L

        private const val DETAIL_PERMISSION_DIALOG = "Dialog izin atau pengaturan dibuka aplikasi ujian"
        private const val DETAIL_SCREEN_OFF = "Layar perangkat dimatikan atau terkunci"
        private const val DETAIL_FOCUS_LOST_BRIEF = "Fokus jendela hilang sebentar"
        private const val DETAIL_OVERLAY_OR_SHADE = "Fokus jendela hilang lebih dari 3 detik"
        private const val DETAIL_CALL_OR_MIC = "Panggilan telepon atau perekam suara aktif"
        private const val DETAIL_SCREEN_SHARE = "Layar kedua atau mode presentasi terdeteksi"
        private const val DETAIL_LEFT_APP = "Siswa keluar dari aplikasi ujian"
    }
}
