package id.sch.mts241alikhlash.exam.data.auth

import id.sch.mts241alikhlash.exam.api.identity.apis.NativeAuthenticationApi
import id.sch.mts241alikhlash.exam.api.identity.models.NativeLoginDto
import id.sch.mts241alikhlash.exam.api.identity.models.NativeLogoutDto
import id.sch.mts241alikhlash.exam.data.remote.ApiException
import id.sch.mts241alikhlash.exam.data.remote.guarded
import id.sch.mts241alikhlash.exam.data.remote.unwrapData
import id.sch.mts241alikhlash.exam.data.remote.unwrapUnit

enum class SupervisorResult { Approved, Denied, Offline }

class SupervisorVerifier(
    private val api: NativeAuthenticationApi,
    private val deviceId: () -> String,
) {
    suspend fun verify(
        identifier: String,
        password: String,
    ): SupervisorResult {
        if (identifier.isBlank() || password.isEmpty()) return SupervisorResult.Denied
        return try {
            val response =
                guarded {
                    api
                        .nativeAuthControllerLogin(
                            NativeLoginDto(identifier.trim(), password, deviceId()),
                        ).unwrapData { it.data }
                }
            response.refreshToken?.let { token ->
                runCatching { api.nativeAuthControllerLogout(NativeLogoutDto(token)).unwrapUnit() }
            }
            if (ROLE_TEACHER in response.user.roles) SupervisorResult.Approved else SupervisorResult.Denied
        } catch (e: ApiException) {
            if (e.statusCode == 0) SupervisorResult.Offline else SupervisorResult.Denied
        }
    }
}
