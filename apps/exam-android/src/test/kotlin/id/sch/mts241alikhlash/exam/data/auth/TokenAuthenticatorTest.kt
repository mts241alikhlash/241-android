package id.sch.mts241alikhlash.exam.data.auth

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TokenAuthenticatorTest {
    private val api = FakeAuthApi()
    private val store = InMemoryTokenStore()
    private val repo = AuthRepository(api, store, deviceId = { "device-1" })
    private var examActive = false
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        runBlocking {
            store.save(
                StoredAuth(
                    AuthTokens("access-1", "refresh-1", Long.MAX_VALUE),
                    AuthUser("u1", "Ahmad", setOf("STUDENT")),
                ),
            )
        }
        server = MockWebServer().also { it.start() }
        client =
            OkHttpClient
                .Builder()
                .addInterceptor(AuthInterceptor(repo))
                .authenticator(TokenAuthenticator(repo) { examActive })
                .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun get(path: String) = client.newCall(Request.Builder().url(server.url(path)).build()).execute()

    @Test
    fun adds_bearer_header() {
        server.enqueue(MockResponse().setResponseCode(200))

        get("/me/exams").close()

        assertEquals("Bearer access-1", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun skips_header_for_auth_native_paths() {
        server.enqueue(MockResponse().setResponseCode(200))

        get("/auth/native/login").close()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun retries_once_with_new_token_after_401() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(200))

        val response = get("/me/exams")

        assertEquals(200, response.code)
        server.takeRequest()
        assertEquals("Bearer access-2", server.takeRequest().getHeader("Authorization"))
        assertEquals(1, api.refreshCalls)
    }

    @Test
    fun gives_up_when_retry_also_401() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(401))

        val response = get("/me/exams")

        assertEquals(401, response.code)
        assertEquals(2, server.requestCount)
        assertEquals(1, api.refreshCalls)
    }

    @Test
    fun does_not_clear_store_when_exam_active_and_refresh_fails() {
        examActive = true
        api.refreshResult = { throw httpError(401) }
        server.enqueue(MockResponse().setResponseCode(401))

        val response = get("/me/exams")

        assertEquals(401, response.code)
        assertNotNull(store.value)
    }

    @Test
    fun clears_store_when_no_exam_active_and_refresh_rejected() {
        api.refreshResult = { throw httpError(401) }
        server.enqueue(MockResponse().setResponseCode(401))

        get("/me/exams").close()

        assertNull(store.value)
    }
}
