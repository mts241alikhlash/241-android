package id.sch.mts241alikhlash.exam.data.remote

import id.sch.mts241alikhlash.exam.data.model.ExamSummaryDto
import id.sch.mts241alikhlash.exam.data.model.StartSessionRequest
import id.sch.mts241alikhlash.exam.data.model.StartedSessionDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ExamCatalogApiService {
    @GET("me/exams")
    suspend fun listExams(): List<ExamSummaryDto>

    @POST("me/exams/{examId}/sessions")
    suspend fun startSession(
        @Path("examId") examId: String,
        @Body request: StartSessionRequest,
    ): StartedSessionDto
}
