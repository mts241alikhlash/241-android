package id.sch.mts241alikhlash.cbt.kiosk

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ActiveInterference { CALL_OR_MIC, SCREEN_SHARE }

data class InterferenceState(
    val callOrMic: Boolean = false,
    val screenShare: Boolean = false
) {
    val any: Boolean get() = callOrMic || screenShare

    val dominant: ActiveInterference?
        get() = when {
            callOrMic -> ActiveInterference.CALL_OR_MIC
            screenShare -> ActiveInterference.SCREEN_SHARE
            else -> null
        }
}

class AudioAndDisplayGuard(context: Context) {

    private val appContext = context.applicationContext
    private val audio = appContext.getSystemService(AudioManager::class.java)
    private val displays = appContext.getSystemService(DisplayManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(InterferenceState())
    val state: StateFlow<InterferenceState> = _state.asStateFlow()

    private var focusRequest: AudioFocusRequest? = null
    private var started = false
    private var micActive = false
    private var displayActive = false

    private val recordingCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: MutableList<AudioRecordingConfiguration>) {
            evaluateMic(configs.isNotEmpty())
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = evaluateDisplays()
        override fun onDisplayRemoved(displayId: Int) = evaluateDisplays()
        override fun onDisplayChanged(displayId: Int) = evaluateDisplays()
    }

    private val micPoll = object : Runnable {
        override fun run() {
            if (!started) return
            evaluateMic(audio?.activeRecordingConfigurations?.isNotEmpty() == true)
            handler.postDelayed(this, MIC_POLL_MS)
        }
    }

    fun start() {
        if (started) return
        started = true
        requestExclusiveAudioFocus()
        runCatching { audio?.registerAudioRecordingCallback(recordingCallback, handler) }
        runCatching { displays?.registerDisplayListener(displayListener, handler) }
        handler.post(micPoll)
        evaluateDisplays()
        evaluateMic(audio?.activeRecordingConfigurations?.isNotEmpty() == true)
    }

    fun stop() {
        if (!started) return
        started = false
        handler.removeCallbacks(micPoll)
        runCatching { audio?.unregisterAudioRecordingCallback(recordingCallback) }
        runCatching { displays?.unregisterDisplayListener(displayListener) }
        abandonAudioFocus()
        micActive = false
        displayActive = false
        _state.value = InterferenceState()
    }

    private fun requestExclusiveAudioFocus() {
        val manager = audio ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { }
                .build()
            focusRequest = request
            runCatching { manager.requestAudioFocus(request) }
            return
        }
        runCatching {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                { },
                AudioManager.STREAM_SYSTEM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE,
            )
        }
    }

    private fun abandonAudioFocus() {
        val manager = audio ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { runCatching { manager.abandonAudioFocusRequest(it) } }
        } else {
            runCatching {
                @Suppress("DEPRECATION")
                manager.abandonAudioFocus { }
            }
        }
        focusRequest = null
    }

    private fun evaluateMic(recordingActive: Boolean) {
        val callActive = audio?.mode != null && audio.mode != AudioManager.MODE_NORMAL
        micActive = recordingActive || callActive
        publish()
    }

    private fun evaluateDisplays() {
        val manager = displays ?: return
        displayActive = manager.displays.any { it.displayId != Display.DEFAULT_DISPLAY }
        publish()
    }

    private fun publish() {
        _state.value = InterferenceState(callOrMic = micActive, screenShare = displayActive)
    }

    private companion object {
        const val MIC_POLL_MS = 1_000L
    }
}
