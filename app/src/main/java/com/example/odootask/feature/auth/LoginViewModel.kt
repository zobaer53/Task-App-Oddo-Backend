package com.example.odootask.feature.auth

import androidx.lifecycle.viewModelScope
import com.example.odootask.core.mvi.MviViewModel
import com.example.odootask.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : MviViewModel<LoginUiState, LoginUiEvent, LoginUiEffect>(LoginUiState()) {

    override fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.UsernameChanged -> updateState { copy(username = event.v) }
            is LoginUiEvent.PasswordChanged -> updateState { copy(password = event.v) }
            LoginUiEvent.PasswordVisibilityToggled ->
                updateState { copy(isPasswordVisible = !isPasswordVisible) }
            LoginUiEvent.ErrorDismissed -> updateState { copy(errorMessage = null) }
            LoginUiEvent.LoginClicked -> login()
        }
    }

    private fun login() {
        if (state.value.isLoading) return
        updateState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val s = state.value
            val result = loginUseCase(s.username, s.password)
            result.fold(
                onSuccess = {
                    updateState { copy(isLoading = false) }
                    sendEffect(LoginUiEffect.NavigateToTasks)
                },
                onFailure = { e ->
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Invalid credentials. Please try again.",
                        )
                    }
                },
            )
        }
    }
}
