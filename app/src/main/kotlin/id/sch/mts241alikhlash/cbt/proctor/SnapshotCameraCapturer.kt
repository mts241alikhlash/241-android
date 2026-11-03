package id.sch.mts241alikhlash.cbt.proctor

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SnapshotCameraCapturer(private val context: Context) {

    private var executor: ExecutorService? = null

    @Volatile
    private var latest: ByteArray? = null

    @Volatile
    private var latestAt: Long = 0L

    @Volatile
    private var provider: ProcessCameraProvider? = null

    @Volatile
    private var capture: ImageCapture? = null

    fun start(owner: LifecycleOwner, onBlocked: (String) -> Unit = {}) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val cameraProvider = runCatching { future.get() }.getOrElse {
                onBlocked("hardware")
                return@addListener
            }
            val worker = Executors.newSingleThreadExecutor()
            val selector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        android.util.Size(WIDTH, HEIGHT),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                    ),
                )
                .build()
            val useCase = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setResolutionSelector(selector)
                .build()
            val bound = runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    owner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    useCase,
                )
            }.isSuccess
            if (bound) {
                provider = cameraProvider
                executor = worker
                capture = useCase
            } else {
                worker.shutdown()
                onBlocked("hardware")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private val inFlight = java.util.concurrent.atomic.AtomicBoolean(false)

    fun captureNow() {
        val useCase = capture ?: return
        val worker = executor ?: return
        if (!inFlight.compareAndSet(false, true)) return
        val file = java.io.File(context.cacheDir, "proctor-${SystemClock.elapsedRealtime()}.jpg")
        useCase.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            worker,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    inFlight.set(false)
                    val bytes = runCatching { file.readBytes() }.getOrNull()
                    if (bytes != null) {
                        latest = bytes
                        latestAt = SystemClock.elapsedRealtime()
                    }
                    file.delete()
                }

                override fun onError(exception: ImageCaptureException) {
                    inFlight.set(false)
                    file.delete()
                    android.util.Log.w(TAG, "capture failed: ${exception.imageCaptureError}", exception)
                }
            },
        )
    }

    fun latestJpeg(): ByteArray? = latest

    fun latestAt(): Long = latestAt

    fun clear() {
        latest = null
        latestAt = 0L
    }

    fun stop() {
        clear()
        runCatching { provider?.unbindAll() }
        provider = null
        capture = null
        executor?.shutdown()
        runCatching { executor?.awaitTermination(2, TimeUnit.SECONDS) }
        executor = null
    }

    private companion object {
        const val TAG = "ProctorCapture"
        const val WIDTH = 320
        const val HEIGHT = 240
    }
}
