package id.sch.mts241alikhlash.exam.proctor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SnapshotCameraCapturer(
    private val context: Context,
) {
    private var executor: ExecutorService? = null

    @Volatile
    private var latest: ByteArray? = null

    @Volatile
    private var latestAt: Long = 0L

    @Volatile
    private var provider: ProcessCameraProvider? = null

    @Volatile
    private var capture: ImageCapture? = null

    fun start(
        owner: LifecycleOwner,
        onBlocked: (String) -> Unit = {},
    ) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val cameraProvider =
                runCatching { future.get() }.getOrElse {
                    onBlocked("hardware")
                    return@addListener
                }
            val worker = Executors.newSingleThreadExecutor()
            val selector =
                ResolutionSelector
                    .Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            android.util.Size(WIDTH, HEIGHT),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                        ),
                    ).build()
            val useCase =
                ImageCapture
                    .Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setResolutionSelector(selector)
                    .build()
            val bound =
                runCatching {
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

    private val inFlight =
        java.util.concurrent.atomic
            .AtomicBoolean(false)

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
                    resize(file, worker)
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

    private fun resize(
        file: java.io.File,
        worker: ExecutorService,
    ) {
        worker.execute {
            val raw = runCatching { file.readBytes() }.getOrNull()
            file.delete()
            if (raw == null) return@execute
            val jpeg = shrink(raw) ?: return@execute
            latest = jpeg
            latestAt = SystemClock.elapsedRealtime()
        }
    }

    private fun shrink(raw: ByteArray): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options =
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            }
        val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size, options) ?: return null
        val scaled = scaleDown(decoded)
        try {
            var quality = SnapshotQuality.LADDER.first()
            while (true) {
                val encoded = encode(scaled, quality)
                if (encoded.size <= SnapshotQuality.MAX_BYTES) return encoded
                quality =
                    SnapshotQuality.nextQuality(quality, encoded.size, SnapshotQuality.MAX_BYTES)
                        ?: return null
            }
        } finally {
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
        }
    }

    private fun sampleSize(
        width: Int,
        height: Int,
    ): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= SnapshotQuality.MAX_LONG_SIDE) sample *= 2
        return sample
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longSide = maxOf(bitmap.width, bitmap.height)
        if (longSide <= SnapshotQuality.MAX_LONG_SIDE) return bitmap
        val ratio = SnapshotQuality.MAX_LONG_SIDE.toFloat() / longSide
        return bitmap.scale(
            maxOf(1, (bitmap.width * ratio).toInt()),
            maxOf(1, (bitmap.height * ratio).toInt()),
        )
    }

    private fun encode(
        bitmap: Bitmap,
        quality: Int,
    ): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return output.toByteArray()
    }

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
        const val WIDTH = 1280
        const val HEIGHT = 720
    }
}
