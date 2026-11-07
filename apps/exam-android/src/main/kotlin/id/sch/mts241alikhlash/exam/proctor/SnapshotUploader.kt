package id.sch.mts241alikhlash.exam.proctor

import id.sch.mts241alikhlash.exam.data.model.CameraBlockedBody
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

interface ProctorSnapshotApi {
    @Multipart
    @POST("student-exams/{sessionId}/proctor-snapshot")
    suspend fun uploadSnapshot(
        @Path("sessionId") sessionId: String,
        @Part("kind") kind: RequestBody,
        @Part("capturedAt") capturedAt: RequestBody,
        @Part file: MultipartBody.Part,
    )

    @POST("student-exams/{sessionId}/camera-blocked")
    suspend fun reportCameraBlocked(
        @Path("sessionId") sessionId: String,
        @Body request: CameraBlockedBody,
    )
}

class SnapshotUploader(
    private val api: ProctorSnapshotApi,
    private val log: (String) -> Unit = {},
) {
    suspend fun upload(
        sessionId: String,
        kind: SnapshotKind,
        jpeg: ByteArray,
        capturedAtMillis: Long,
    ): Boolean =
        try {
            api.uploadSnapshot(
                sessionId = sessionId,
                kind = kind.name.toRequestBody(TEXT),
                capturedAt = isoMillis(capturedAtMillis).toRequestBody(TEXT),
                file =
                    MultipartBody.Part.createFormData(
                        "file",
                        "snapshot.jpg",
                        jpeg.toRequestBody(JPEG),
                    ),
            )
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
            api.reportCameraBlocked(sessionId, CameraBlockedBody(reason))
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
