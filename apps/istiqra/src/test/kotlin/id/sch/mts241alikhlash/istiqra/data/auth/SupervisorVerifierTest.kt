package id.sch.mts241alikhlash.istiqra.data.auth

import id.sch.mts241alikhlash.istiqra.api.identity.apis.NativeAuthenticationApi
import id.sch.mts241alikhlash.istiqra.api.identity.models.NativeAuthControllerLogin200Response
import id.sch.mts241alikhlash.istiqra.api.identity.models.NativeLoginDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class SupervisorVerifierTest {
    private val api = FakeAuthApi()
    private val verifier = SupervisorVerifier(api) { "device-1" }

    @Test
    fun verify_approved_for_teacher_role() =
        runTest {
            api.loginResult = { authResponse(roles = listOf("TEACHER")) }

            assertEquals(SupervisorResult.Approved, verifier.verify("guru", "secret"))
        }

    @Test
    fun verify_denied_for_student_role() =
        runTest {
            api.loginResult = { authResponse(roles = listOf("STUDENT")) }

            assertEquals(SupervisorResult.Denied, verifier.verify("ahmad", "secret"))
        }

    @Test
    fun verify_denied_on_401() =
        runTest {
            api.loginResult = { throw httpError(401) }

            assertEquals(SupervisorResult.Denied, verifier.verify("guru", "salah"))
        }

    @Test
    fun verify_offline_on_network_error() =
        runTest {
            val offline =
                object : NativeAuthenticationApi by api {
                    override suspend fun nativeAuthControllerLogin(nativeLoginDto: NativeLoginDto): Response<NativeAuthControllerLogin200Response> = throw IOException("offline")
                }

            assertEquals(
                SupervisorResult.Offline,
                SupervisorVerifier(offline) { "device-1" }.verify("guru", "secret"),
            )
        }

    @Test
    fun verify_denies_blank_credentials_without_calling_the_server() =
        runTest {
            var called = false
            api.loginResult = {
                called = true
                authResponse(roles = listOf("TEACHER"))
            }

            assertEquals(SupervisorResult.Denied, verifier.verify(" ", ""))
            assertEquals(false, called)
        }

    @Test
    fun verify_revokes_the_session_it_created() =
        runTest {
            api.loginResult = { authResponse(roles = listOf("TEACHER")) }
            verifier.verify("guru", "secret")

            api.loginResult = { authResponse(roles = listOf("STUDENT")) }
            verifier.verify("ahmad", "secret")

            assertEquals(2, api.logoutCalls)
        }
}
