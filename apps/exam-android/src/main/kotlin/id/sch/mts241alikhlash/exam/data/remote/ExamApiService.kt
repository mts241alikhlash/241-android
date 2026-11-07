package id.sch.mts241alikhlash.exam.data.remote

import id.sch.mts241alikhlash.exam.data.model.RecordSessionEventsRequest
import id.sch.mts241alikhlash.exam.data.model.RecordSessionEventsResponse
import id.sch.mts241alikhlash.exam.data.model.ResumeSessionRequest
import id.sch.mts241alikhlash.exam.data.model.ResumedSessionDto
import id.sch.mts241alikhlash.exam.data.model.SaveAnswersBatchRequest
import id.sch.mts241alikhlash.exam.data.model.SaveAnswersBatchResultDto
import id.sch.mts241alikhlash.exam.data.model.SavedAnswerDto
import id.sch.mts241alikhlash.exam.data.model.SessionHeartbeatResponse
import id.sch.mts241alikhlash.exam.data.model.SessionPaperDto
import id.sch.mts241alikhlash.exam.data.model.SubmitRequest
import id.sch.mts241alikhlash.exam.data.model.SubmittedSessionDto
import id.sch.mts241alikhlash.exam.data.model.SuspensionRequest
import id.sch.mts241alikhlash.exam.data.model.SuspensionResponseDto
import id.sch.mts241alikhlash.exam.data.model.ViolationCountDto
import id.sch.mts241alikhlash.exam.data.model.ViolationReport
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ExamApiService {
    @GET("student-exams/{sessionId}/questions")
    suspend fun getQuestions(
        @Path("sessionId") sessionId: String,
    ): SessionPaperDto

    @GET("student-exams/{sessionId}/answers")
    suspend fun getAnswers(
        @Path("sessionId") sessionId: String,
    ): List<SavedAnswerDto>

    @POST("student-exams/{sessionId}/answers/batch")
    suspend fun saveAnswersBatch(
        @Path("sessionId") sessionId: String,
        @Body request: SaveAnswersBatchRequest,
    ): SaveAnswersBatchResultDto

    @POST("student-exams/{sessionId}/submit")
    suspend fun submit(
        @Path("sessionId") sessionId: String,
        @Body request: SubmitRequest,
    ): SubmittedSessionDto

    @POST("student-exams/{sessionId}/events")
    suspend fun recordSessionEvents(
        @Path("sessionId") sessionId: String,
        @Body request: RecordSessionEventsRequest,
    ): RecordSessionEventsResponse

    @POST("student-exams/{sessionId}/heartbeat")
    suspend fun sessionHeartbeat(
        @Path("sessionId") sessionId: String,
    ): SessionHeartbeatResponse

    @POST("student-exams/{sessionId}/resume")
    suspend fun resumeSession(
        @Path("sessionId") sessionId: String,
        @Body request: ResumeSessionRequest,
    ): ResumedSessionDto

    @POST("student-exams/{sessionId}/violations")
    suspend fun reportViolations(
        @Path("sessionId") sessionId: String,
        @Body request: ViolationReport,
    ): ViolationCountDto

    @POST("student-exams/{sessionId}/suspension")
    suspend fun reportSuspension(
        @Path("sessionId") sessionId: String,
        @Body request: SuspensionRequest,
    ): SuspensionResponseDto
}
