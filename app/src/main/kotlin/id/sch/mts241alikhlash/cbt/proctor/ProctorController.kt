package id.sch.mts241alikhlash.cbt.proctor

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

interface SnapshotSource {
    fun start(owner: LifecycleOwner, onBlocked: (String) -> Unit)
    fun captureNow()
    fun latestJpeg(): ByteArray?
    fun latestAt(): Long
    fun clear()
    fun stop()
}

private class CameraSnapshotSource(context: Context) : SnapshotSource {
    private val capturer = SnapshotCameraCapturer(context)

    override fun start(owner: LifecycleOwner, onBlocked: (String) -> Unit) =
        capturer.start(owner, onBlocked)

    override fun captureNow() = capturer.captureNow()

    override fun latestJpeg(): ByteArray? = capturer.latestJpeg()

    override fun latestAt(): Long = capturer.latestAt()

    override fun clear() = capturer.clear()

    override fun stop() = capturer.stop()
}

class ProctorController(
    private val api: ProctorSnapshotApi,
    private val isAnswerSyncBusy: () -> Boolean,
    private val source: SnapshotSource,
    private val log: (String) -> Unit = {},
) {

    private val uploader = SnapshotUploader(api, log)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scheduler = SnapshotScheduler(isAnswerSyncBusy)
    private var loop: Job? = null
    private var observer: LifecycleEventObserver? = null
    private var owner: LifecycleOwner? = null
    private var sessionId: String = ""

    @Volatile
    private var lastUploadedAt: Long = 0L

    fun start(owner: LifecycleOwner, sessionId: String) {
        if (!ProctorFlags.ENABLED) return
        if (loop?.isActive == true) return
        unbind()

        this.owner = owner
        this.sessionId = sessionId
        lastUploadedAt = 0L
        scheduler.reset()
        source.start(owner) { reason -> blocked(reason) }

        var stopped = false
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    stopped = true
                    source.clear()
                }
                Lifecycle.Event.ON_START -> {
                    if (stopped) {
                        stopped = false
                        scheduler.onAppReturn()
                    }
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(lifecycleObserver)
        observer = lifecycleObserver

        loop = scope.launch {
            while (isActive) {
                tick()
                delay(SnapshotScheduler.INTERVAL_MS)
            }
        }
    }

    suspend fun tick() {
        source.captureNow()
        delay(CAPTURE_WAIT_MS)
        val frameAt = source.latestAt()
        if (frameAt <= lastUploadedAt) return
        val jpeg = source.latestJpeg() ?: return
        val kind = scheduler.nextKind() ?: return
        if (uploader.upload(sessionId, kind, jpeg, System.currentTimeMillis())) {
            lastUploadedAt = frameAt
        } else {
            scheduler.onUploadFailed(kind)
        }
    }

    fun stop() {
        loop?.cancel()
        loop = null
        unbind()
        source.stop()
    }

    fun blocked(sessionId: String, reason: String) {
        scope.launch { uploader.reportBlocked(sessionId, reason) }
    }

    private fun blocked(reason: String) {
        if (sessionId.isEmpty()) return
        blocked(sessionId, reason)
    }

    private fun unbind() {
        val current = owner ?: return
        observer?.let { current.lifecycle.removeObserver(it) }
        observer = null
        owner = null
    }

    companion object {
        private const val CAPTURE_WAIT_MS = 1_200L

        fun camera(
            context: Context,
            api: ProctorSnapshotApi,
            isAnswerSyncBusy: () -> Boolean,
        ) = ProctorController(
            api,
            isAnswerSyncBusy,
            CameraSnapshotSource(context),
            log = { message -> android.util.Log.w("ProctorUpload", message) },
        )
    }
}
