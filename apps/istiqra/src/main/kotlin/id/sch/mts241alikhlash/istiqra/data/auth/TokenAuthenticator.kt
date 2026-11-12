package id.sch.mts241alikhlash.istiqra.data.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val repository: AuthRepository,
    private val examActive: () -> Boolean,
) : Authenticator {
    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        if (response.request.header("Authorization") == null) return null
        if (response.priorResponse != null) return null
        val stale = response.request.header("Authorization")?.removePrefix("Bearer ")
        val tokens =
            runBlocking {
                repository.refresh(clearOnRejected = !examActive(), staleAccessToken = stale)
            } ?: return null
        return response.request
            .newBuilder()
            .header("Authorization", "Bearer ${tokens.accessToken}")
            .build()
    }
}
