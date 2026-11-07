package id.sch.mts241alikhlash.exam.data.auth

import id.sch.mts241alikhlash.exam.data.remote.ApiEnvelope
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/native/login")
    suspend fun login(
        @Body request: LoginRequest,
    ): ApiEnvelope<AuthResponseDto>

    @POST("auth/native/refresh")
    suspend fun refresh(
        @Body request: RefreshRequest,
    ): ApiEnvelope<AuthResponseDto>

    @POST("auth/native/logout")
    suspend fun logout(
        @Body request: LogoutRequest,
    ): ApiEnvelope<JsonElement>
}
