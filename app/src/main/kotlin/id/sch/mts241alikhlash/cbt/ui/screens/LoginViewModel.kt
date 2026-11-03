package id.sch.mts241alikhlash.cbt.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.sch.mts241alikhlash.cbt.data.auth.AuthRepository
import id.sch.mts241alikhlash.cbt.data.auth.AuthUser
import id.sch.mts241alikhlash.cbt.data.auth.ROLE_STUDENT
import id.sch.mts241alikhlash.cbt.data.remote.ApiException
import id.sch.mts241alikhlash.cbt.data.remote.userMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val identifierError: String? = null,
    val passwordError: String? = null
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _loggedIn = MutableStateFlow<AuthUser?>(null)
    val loggedIn: StateFlow<AuthUser?> = _loggedIn.asStateFlow()

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun consumed() {
        _loggedIn.value = null
    }

    fun clearFieldErrors() {
        val current = _state.value
        if (current.identifierError == null && current.passwordError == null) return
        _state.value = current.copy(identifierError = null, passwordError = null)
    }

    fun submit(identifier: String, password: String) {
        if (_state.value.loading) return
        val identifierError = if (identifier.isBlank()) "NIS wajib diisi" else null
        val passwordError = if (password.isEmpty()) "Kata sandi wajib diisi" else null
        if (identifierError != null || passwordError != null) {
            _state.value = LoginUiState(
                identifierError = identifierError,
                passwordError = passwordError
            )
            return
        }
        _state.value = LoginUiState(loading = true)
        viewModelScope.launch {
            try {
                val user = auth.login(identifier, password, ROLE_STUDENT)
                _state.value = LoginUiState()
                _loggedIn.value = user
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                val api = error as? ApiException
                _state.value = when {
                    api == null -> LoginUiState(error = error.userMessage())
                    api.statusCode == 401 -> LoginUiState(
                        identifierError = "NIS atau kata sandi salah",
                        passwordError = "NIS atau kata sandi salah"
                    )
                    api.statusCode == 403 -> LoginUiState(
                        identifierError = api.message.ifBlank { "Akun ini tidak dapat masuk di sini." }
                    )
                    api.statusCode == 404 -> LoginUiState(
                        identifierError = api.message.ifBlank { "NIS tidak terdaftar." }
                    )
                    api.statusCode == 429 -> LoginUiState(
                        error = "Terlalu banyak percobaan. Tunggu sebentar lalu coba lagi."
                    )
                    api.statusCode in 500..599 -> LoginUiState(
                        error = "Server sedang bermasalah. Coba lagi beberapa saat."
                    )
                    else -> LoginUiState(error = error.userMessage())
                }
            }
        }
    }
}
