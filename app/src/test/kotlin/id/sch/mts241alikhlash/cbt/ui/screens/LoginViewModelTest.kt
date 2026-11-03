package id.sch.mts241alikhlash.cbt.ui.screens

import id.sch.mts241alikhlash.cbt.MainDispatcherRule
import id.sch.mts241alikhlash.cbt.data.auth.AuthRepository
import id.sch.mts241alikhlash.cbt.data.auth.FakeAuthApi
import id.sch.mts241alikhlash.cbt.data.auth.InMemoryTokenStore
import id.sch.mts241alikhlash.cbt.data.auth.authResponse
import id.sch.mts241alikhlash.cbt.data.auth.httpError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val api = FakeAuthApi()
    private val store = InMemoryTokenStore()
    private val repo = AuthRepository(api, store, { "device-1" })
    private val viewModel = LoginViewModel(repo)

    @Test
    fun login_success_exposes_user() {
        viewModel.submit("ahmad", "secret")

        assertEquals("u1", viewModel.loggedIn.value?.id)
        assertEquals(LoginUiState(), viewModel.state.value)
    }

    @Test
    fun login_with_teacher_account_shows_message_and_no_user() {
        api.loginResult = { authResponse(roles = listOf("TEACHER")) }

        viewModel.submit("guru", "secret")

        assertNull(viewModel.loggedIn.value)
        assertEquals(
            "Akun ini tidak dapat masuk di sini.",
            viewModel.state.value.identifierError
        )
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun login_error_message_from_server_is_shown() {
        val failing = object : id.sch.mts241alikhlash.cbt.data.auth.AuthApiService by api {
            override suspend fun login(
                request: id.sch.mts241alikhlash.cbt.data.auth.LoginRequest
            ): id.sch.mts241alikhlash.cbt.data.remote.ApiEnvelope<id.sch.mts241alikhlash.cbt.data.auth.AuthResponseDto> =
                throw httpError(401)
        }
        val vm = LoginViewModel(AuthRepository(failing, store, { "device-1" }))

        vm.submit("ahmad", "salah")

        assertNull(vm.loggedIn.value)
        assertEquals("NIS atau kata sandi salah", vm.state.value.identifierError)
        assertEquals("NIS atau kata sandi salah", vm.state.value.passwordError)
    }

    @Test
    fun blank_fields_are_rejected_without_calling_the_server() {
        viewModel.submit("  ", "")

        assertEquals("NIS wajib diisi", viewModel.state.value.identifierError)
        assertEquals("Kata sandi wajib diisi", viewModel.state.value.passwordError)
        assertNull(viewModel.loggedIn.value)
    }

    @Test
    fun non_api_error_shows_generic_message() {
        val failing = object : id.sch.mts241alikhlash.cbt.data.auth.AuthApiService by api {
            override suspend fun login(
                request: id.sch.mts241alikhlash.cbt.data.auth.LoginRequest
            ): id.sch.mts241alikhlash.cbt.data.remote.ApiEnvelope<id.sch.mts241alikhlash.cbt.data.auth.AuthResponseDto> =
                throw IllegalStateException("Keystore internal detail")
        }
        val vm = LoginViewModel(AuthRepository(failing, store, { "device-1" }))

        vm.submit("ahmad", "secret")

        assertEquals("Terjadi kesalahan. Coba lagi.", vm.state.value.error)
    }

    @Test
    fun consumed_clears_logged_in_so_a_later_login_screen_starts_empty() {
        viewModel.submit("ahmad", "secret")

        viewModel.consumed()

        assertNull(viewModel.loggedIn.value)
    }
}
