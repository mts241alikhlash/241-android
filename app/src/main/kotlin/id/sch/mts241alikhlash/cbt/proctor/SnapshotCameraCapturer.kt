package id.sch.mts241alikhlash.cbt.proctor

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.camera2.CaptureRequest
import android.os.SystemClock
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SnapshotCameraCapturer(private val context: Context) {

    private var executor: ExecutorService? = null

    @Volatile
    private var latest: ByteArray? = null

    @Volatile
    private var latestAt: Long = 0L

    @Volatile
    private var lastCompressedAt: Long = 0L

    @Volatile
    private var provider: ProcessCameraProvider? = null

    fun start(owner: LifecycleOwner, onBlocked: (String) -> Unit = {}) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val cameraProvider = runCatching { future.get() }.getOrElse {
                onBlocked("hardware")
                return@addListener
            }
            val worker = Executors.newSingleThreadExecutor()
            val analyzer = ImageAnalysis.Analyzer { image ->
                try {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastCompressedAt < MIN_INTERVAL_MS) return@Analyzer
                    lastCompressedAt = now
                    val bytes = image.jpeg() ?: return@Analyzer
                    latest = bytes
                    latestAt = now
                } finally {
                    image.close()
                }
            }

            fun useCase(capFps: Boolean): ImageAnalysis {
                val builder = ImageAnalysis.Builder()
                    .setTargetResolution(Size(WIDTH, HEIGHT))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                if (capFps) {
                    Camera2Interop.Extender(builder)
                        .setCaptureRequestOption(
                            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                            Range(1, 1),
                        )
                }
                return builder.build().apply { setAnalyzer(worker, analyzer) }
            }

            fun bind(candidate: ImageAnalysis): Boolean = runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    owner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    candidate,
                )
            }.isSuccess

            val bound = bind(useCase(capFps = true)) || bind(useCase(capFps = false))
            if (bound) {
                provider = cameraProvider
                executor = worker
            } else {
                worker.shutdown()
                onBlocked("hardware")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun latestJpeg(): ByteArray? = latest

    fun latestAt(): Long = latestAt

    fun clear() {
        latest = null
        latestAt = 0L
    }

    fun stop() {
        clear()
        lastCompressedAt = 0L
        runCatching { provider?.unbindAll() }
        provider = null
        executor?.shutdown()
        executor = null
    }

    private fun ImageProxy.jpeg(): ByteArray? {
        val yuv = nv21()
        for (quality in QUALITIES) {
            val out = ByteArrayOutputStream()
            val written = runCatching {
                YuvImage(yuv, ImageFormat.NV21, width, height, null)
                    .compressToJpeg(Rect(0, 0, width, height), quality, out)
            }.getOrDefault(false)
            if (written) {
                val bytes = out.toByteArray()
                if (bytes.size <= MAX_BYTES) return bytes
            }
        }
        return null
    }

    private fun ImageProxy.nv21(): ByteArray {
        val ySize = width * height
        val out = ByteArray(ySize + 2 * (width / 2) * (height / 2))
        val y = planes[0]
        var offset = 0
        for (row in 0 until height) {
            val start = row * y.rowStride
            for (col in 0 until width) {
                out[offset++] = y.buffer.get(start + col * y.pixelStride)
            }
        }
        val u = planes[1]
        val v = planes[2]
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                out[offset++] = v.buffer.get(row * v.rowStride + col * v.pixelStride)
                out[offset++] = u.buffer.get(row * u.rowStride + col * u.pixelStride)
            }
        }
        return out
    }

    private companion object {
        const val WIDTH = 320
        const val HEIGHT = 240
        const val MAX_BYTES = 25_000
        const val MIN_INTERVAL_MS = 14_000L
        val QUALITIES = intArrayOf(60, 45, 30)
    }
}
