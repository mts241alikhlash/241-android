package id.sch.mts241alikhlash.exam.data.auth

import id.sch.mts241alikhlash.exam.api.identity.apis.NativeAuthenticationApi
import id.sch.mts241alikhlash.exam.api.identity.models.NativeAuthControllerLogin200Response
import id.sch.mts241alikhlash.exam.api.identity.models.NativeAuthResponseDto
import id.sch.mts241alikhlash.exam.api.identity.models.NativeLoginDto
import id.sch.mts241alikhlash.exam.api.identity.models.NativeLogoutDto
import id.sch.mts241alikhlash.exam.api.identity.models.NativeRefreshDto
import id.sch.mts241alikhlash.exam.api.identity.models.NativeUserDto
import id.sch.mts241alikhlash.exam.data.remote.ApiException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class InMemoryTokenStore : TokenStore {
    var value: StoredAuth? = null

    override suspend fun load(): StoredAuth? = value

    override suspend fun save(auth: StoredAuth) {
        value = auth
    }

    override suspend fun clear() {
        value = null
    }
}

open class FakeAuthApi : NativeAuthenticationApi {
    var loginResult: () -> NativeAuthResponseDto = { authResponse(roles = listOf("STUDENT")) }
    var refreshResult: () -> NativeAuthResponseDto = { authResponse(access = "access-2", refresh = "refresh-2") }
    var logoutFails = false
    var refreshCalls = 0
    var logoutCalls = 0
    var refreshDelayMillis = 0L

    override suspend fun nativeAuthControllerLogin(nativeLoginDto: NativeLoginDto): Response<NativeAuthControllerLogin200Response> = Response.success(NativeAuthControllerLogin200Response(data = loginResult()))

    override suspend fun nativeAuthControllerRefresh(nativeRefreshDto: NativeRefreshDto): Response<NativeAuthControllerLogin200Response> {
        refreshCalls++
        if (refreshDelayMillis > 0) delay(refreshDelayMillis)
        return Response.success(NativeAuthControllerLogin200Response(data = refreshResult()))
    }

    override suspend fun nativeAuthControllerLogout(nativeLogoutDto: NativeLogoutDto): Response<Unit> {
        logoutCalls++
        if (logoutFails) throw IOException("offline")
        return Response.success(Unit)
    }
}

fun authResponse(
    access: String = "access-1",
    refresh: String? = "refresh-1",
    roles: List<String> = listOf("STUDENT"),
) = NativeAuthResponseDto(
    accessToken = access,
    refreshToken = refresh,
    expiresIn = 900L,
    user = NativeUserDto(id = "u1", name = "Ahmad", roles = roles),
)

fun httpError(code: Int): HttpException =
    HttpException(
        Response.error<Any>(
            code,
            """{"statusCode":$code,"message":"x","data":null}""".toResponseBody(),
        ),
    )

class AuthRepositoryTest {
    private val api = FakeAuthApi()
    private val store = InMemoryTokenStore()
    private var clock = 1_000_000L
    private val repo = AuthRepository(api, store, deviceId = { "device-1" }, now = { clock })

    @Test
    fun login_stores_tokens_and_user() =
        runTest {
            val user = repo.login("ahmad", "secret", requiredRole = "STUDENT")

            assertEquals("u1", user.id)
            val stored = store.value!!
            assertEquals("access-1", stored.tokens.accessToken)
            assertEquals("refresh-1", stored.tokens.refreshToken)
            assertEquals(clock + 900_000L, stored.tokens.expiresAtEpochMillis)
            assertTrue(stored.user.isStudent)
        }

    @Test
    fun login_rejects_teacher_when_student_required_and_stores_nothing() =
        runTest {
            api.loginResult = { authResponse(roles = listOf("TEACHER")) }

            val error =
                runCatching { repo.login("guru", "secret", requiredRole = "STUDENT") }
                    .exceptionOrNull() as ApiException

            assertEquals(403, error.statusCode)
            assertEquals("Akun ini tidak dapat masuk di sini.", error.message)
            assertNull(store.value)
        }

    @Test
    fun refresh_rotates_tokens() =
        runTest {
            repo.login("ahmad", "secret", requiredRole = "STUDENT")

            val tokens = repo.refresh()

            assertEquals("access-2", tokens?.accessToken)
            assertEquals("refresh-2", store.value!!.tokens.refreshToken)
            assertEquals("u1", store.value!!.user.id)
        }

    @Test
    fun refresh_without_a_new_refresh_token_keeps_the_one_held() =
        runTest {
            repo.login("ahmad", "secret", requiredRole = "STUDENT")
            api.refreshResult = { authResponse(access = "access-2", refresh = null) }

            val tokens = repo.refresh()

            assertEquals("access-2", tokens?.accessToken)
            assertEquals("refresh-1", store.value!!.tokens.refreshToken)
        }

    @Test
    fun login_without_a_refresh_token_is_an_error_and_stores_nothing() =
        runTest {
            api.loginResult = { authResponse(refresh = null) }

            val error = runCatching { repo.login("ahmad", "secret", requiredRole = "STUDENT") }.exceptionOrNull()

            assertTrue(error is ApiException)
            assertNull(store.value)
        }

    @Test
    fun concurrent_refresh_calls_api_once() =
        runTest {
            repo.login("ahmad", "secret", requiredRole = "STUDENT")
            api.refreshDelayMillis = 100

            val results = listOf(async { repo.refresh() }, async { repo.refresh() }).awaitAll()

            assertEquals(1, api.refreshCalls)
            assertEquals(listOf("access-2", "access-2"), results.map { it?.accessToken })
        }

    @Test
    fun refresh_without_stored_auth_returns_null_and_skips_api() =
        runTest {
            assertNull(repo.refresh())
            assertEquals(0, api.refreshCalls)
        }

    @Test
    fun refresh_clears_store_on_401() =
        runTest {
            repo.login("ahmad", "secret", requiredRole = "STUDENT")
            val failing =
                object : NativeAuthenticationApi by api {
                    override suspend fun nativeAuthControllerRefresh(nativeRefreshDto: NativeRefreshDto): Response<NativeAuthControllerLogin200Response> = throw httpError(401)
                }
            val failingRepo = AuthRepository(failing, store, { "device-1" }, { clock })

            assertNull(failingRepo.refresh())
            assertNull(store.value)
        }

    @Test
    fun refresh_keeps_store_on_network_error() =
        runTest {
            repo.login("ahmad", "secret", requiredRole = "STUDENT")
            val offline =
                object : NativeAuthenticationApi by api {
                    override suspend fun nativeAuthControllerRefresh(nativeRefreshDto: NativeRefreshDto): Response<NativeAuthControllerLogin200Response> = throw IOException("offline")
                }
            val offlineRepo = AuthRepository(offline, store, { "device-1" }, { clock })

            assertNull(offlineRepo.refresh())
            assertNotNull(store.value)
            assertEquals("refresh-1", store.value!!.tokens.refreshToken)
        }

    @Test
    fun logout_clears_store_even_when_api_fails() =
        runTest {
            repo.login("ahmad", "secret", requiredRole = "STUDENT")
            api.logoutFails = true

            repo.logout()

            assertEquals(1, api.logoutCalls)
            assertNull(store.value)
        }

    @Test
    fun late_401_with_stale_access_token_reuses_rotated_tokens() =
        runTest {
            repo.login("ahmad", "secret", requiredRole = "STUDENT")
            repo.refresh()

            val tokens = repo.refresh(staleAccessToken = "access-1")

            assertEquals(1, api.refreshCalls)
            assertEquals("access-2", tokens?.accessToken)
        }

    @Test
    fun rejected_refresh_during_exam_marks_rejected_and_keeps_store() =
        runTest {
            repo.login("ahmad", "secret", requiredRole = "STUDENT")
            api.refreshResult = { throw httpError(401) }

            assertNull(repo.refresh(clearOnRejected = false))

            assertTrue(repo.rejected.value)
            assertNotNull(store.value)
        }

    @Test
    fun refresh_skips_api_while_rejected() =
        runTest {
            repo.login("ahmad", "secret", requiredRole = "STUDENT")
            api.refreshResult = { throw httpError(401) }
            repo.refresh(clearOnRejected = false)
            val callsBefore = api.refreshCalls

            assertNull(repo.refresh(clearOnRejected = false))

            assertEquals(callsBefore, api.refreshCalls)
        }

    @Test
    fun relogin_with_same_user_clears_rejected_and_replaces_tokens() =
        runTest {
            repo.login("ahmad", "secret", requiredRole = "STUDENT")
            api.refreshResult = { throw httpError(401) }
            repo.refresh(clearOnRejected = false)
            api.loginResult = { authResponse(access = "access-9", refresh = "refresh-9") }

            repo.relogin("ahmad", "secret")

            assertEquals(false, repo.rejected.value)
            assertEquals("access-9", store.value!!.tokens.accessToken)
        }

    @Test
    fun relogin_with_different_user_is_refused_and_keeps_rejected() =
        runTest {
            repo.login("ahmad", "secret", requiredRole = "STUDENT")
            api.refreshResult = { throw httpError(401) }
            repo.refresh(clearOnRejected = false)
            api.loginResult = {
                authResponse(access = "access-9").copy(user = NativeUserDto("u2", "Budi", listOf("STUDENT")))
            }

            val error = runCatching { repo.relogin("budi", "secret") }.exceptionOrNull() as ApiException

            assertEquals("Akun berbeda dengan akun ujian.", error.message)
            assertTrue(repo.rejected.value)
            assertEquals("access-1", store.value!!.tokens.accessToken)
        }

    @Test
    fun rejected_teacher_login_revokes_the_issued_session() =
        runTest {
            api.loginResult = { authResponse(roles = listOf("TEACHER")) }

            runCatching { repo.login("guru", "secret", requiredRole = "STUDENT") }

            assertEquals(1, api.logoutCalls)
        }

    @Test
    fun relogin_with_different_user_revokes_the_issued_session() =
        runTest {
            repo.login("ahmad", "secret", requiredRole = "STUDENT")
            api.loginResult = {
                authResponse(access = "access-9").copy(user = NativeUserDto("u2", "Budi", listOf("STUDENT")))
            }

            runCatching { repo.relogin("budi", "secret") }

            assertEquals(1, api.logoutCalls)
        }

    @Test
    fun currentStudent_returns_stored_student() =
        runTest {
            repo.login("ahmad", "secret", requiredRole = "STUDENT")

            assertEquals("u1", repo.currentStudent()?.id)
        }

    @Test
    fun currentStudent_is_null_for_teacher_and_for_nothing() =
        runTest {
            assertNull(repo.currentStudent())
            store.value =
                StoredAuth(
                    AuthTokens("a", "r", 1L),
                    AuthUser("t1", "Guru", setOf("TEACHER")),
                )

            assertNull(repo.currentStudent())
        }
}
