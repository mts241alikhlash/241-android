package id.sch.mts241alikhlash.istiqra.data.auth

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLoggingTest {
    @Test
    fun auth_logging_never_prints_request_or_response_bodies() {
        val lines = mutableListOf<String>()
        val server = MockWebServer().also { it.start() }
        server.enqueue(MockResponse().setBody("""{"data":{"refresh_token":"refresh-secret-value"}}"""))
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(authLoggingInterceptor(enabled = true) { lines += it })
                .build()

        client
            .newCall(
                Request
                    .Builder()
                    .url(server.url("/auth/native/login"))
                    .header("Authorization", "Bearer access-secret")
                    .post("""{"password":"hunter2-secret"}""".toRequestBody())
                    .build(),
            ).execute()
            .close()
        server.shutdown()

        val output = lines.joinToString("\n")
        assertTrue(output.contains("/auth/native/login"))
        assertFalse(output.contains("hunter2-secret"))
        assertFalse(output.contains("refresh-secret-value"))
        assertFalse(output.contains("access-secret"))
    }
}
