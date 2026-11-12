package id.sch.mts241alikhlash.istiqra.proctor

import id.sch.mts241alikhlash.istiqra.api.exam.apis.StudentExamsApi
import id.sch.mts241alikhlash.istiqra.api.exam.models.CameraBlockedDto
import id.sch.mts241alikhlash.istiqra.data.remote.unwrapData
import id.sch.mts241alikhlash.istiqra.data.remote.unwrapUnit
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class SnapshotUploader(
    private val api: StudentExamsApi,
    private val deviceId: () -> String = { "" },
    private val log: (String) -> Unit = {},
) {
    suspend fun upload(
        sessionId: String,
        kind: SnapshotKind,
        jpeg: ByteArray,
        capturedAtMillis: Long,
    ): Boolean =
        try {
            api
                .sessionExamControllerSnapshot(
                    xDeviceId = deviceId(),
                    sessionId = sessionId,
                    kind = kind.name.toRequestBody(TEXT),
                    capturedAt = isoMillis(capturedAtMillis).toRequestBody(TEXT),
                    file =
                        MultipartBody.Part.createFormData(
                            "file",
                            "snapshot.jpg",
                            jpeg.toRequestBody(JPEG),
                        ),
                ).unwrapData { it.data }
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            log("snapshot upload failed: ${failure.message}")
            false
        }

    suspend fun reportBlocked(
        sessionId: String,
        reason: String,
    ): Boolean =
        try {
            api
                .sessionExamControllerCameraBlocked(
                    deviceId(),
                    sessionId,
                    CameraBlockedDto(CameraBlockedDto.Reason.entries.first { it.value == reason }),
                ).unwrapUnit()
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            false
        }

    companion object {
        private val TEXT = "text/plain".toMediaType()
        private val JPEG = "image/jpeg".toMediaType()

        fun isoMillis(millis: Long): String =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(millis)
    }
}
