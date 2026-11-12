package id.sch.mts241alikhlash.istiqra.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.sch.mts241alikhlash.istiqra.data.remote.ApiException
import id.sch.mts241alikhlash.istiqra.data.remote.GENERIC_ERROR_MESSAGE
import id.sch.mts241alikhlash.istiqra.data.repository.StudentProfile
import id.sch.mts241alikhlash.istiqra.data.repository.StudentProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Content(
        val profile: StudentProfile,
    ) : ProfileUiState

    data class Error(
        val message: String,
    ) : ProfileUiState
}

class ProfileViewModel(
    private val repository: StudentProfileRepository,
    private val fallbackName: String,
) : ViewModel() {
    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (_state.value !is ProfileUiState.Content) _state.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                _state.value = ProfileUiState.Content(repository.load(fallbackName))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: ApiException) {
                keepOrFail(error.message)
            } catch (error: Exception) {
                keepOrFail(GENERIC_ERROR_MESSAGE)
            }
        }
    }

    private fun keepOrFail(message: String) {
        if (_state.value !is ProfileUiState.Content) _state.value = ProfileUiState.Error(message)
    }
}
