package id.sch.mts241alikhlash.exam.data.auth

import id.sch.mts241alikhlash.exam.data.remote.ApiException
import id.sch.mts241alikhlash.exam.data.remote.apiCall

enum class SupervisorResult { Approved, Denied, Offline }

class SupervisorVerifier(
    private val api: AuthApiService,
    private val deviceId: () -> String,
) {
    suspend fun verify(
        identifier: String,
        password: String,
    ): SupervisorResult {
        if (identifier.isBlank() || password.isEmpty()) return SupervisorResult.Denied
        return try {
            val response = apiCall { api.login(LoginRequest(identifier.trim(), password, deviceId())) }
            response.refreshToken?.let { token -> runCatching { api.logout(LogoutRequest(token)) } }
            if (ROLE_TEACHER in response.user.roles) SupervisorResult.Approved else SupervisorResult.Denied
        } catch (e: ApiException) {
            if (e.statusCode == 0) SupervisorResult.Offline else SupervisorResult.Denied
        }
    }
}
