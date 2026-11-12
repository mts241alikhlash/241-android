package id.sch.mts241alikhlash.istiqra.data.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

const val AUTH_PATH_MARKER = "auth/native/"

class AuthInterceptor(
    private val repository: AuthRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath.contains(AUTH_PATH_MARKER)) return chain.proceed(request)
        val token =
            runBlocking { repository.current() }?.tokens?.accessToken
                ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder().header("Authorization", "Bearer $token").build(),
        )
    }
}
