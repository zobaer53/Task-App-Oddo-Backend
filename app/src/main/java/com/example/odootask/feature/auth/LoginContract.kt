package com.example.odootask.feature.auth

import com.example.odootask.core.mvi.MviUiEffect
import com.example.odootask.core.mvi.MviUiEvent
import com.example.odootask.core.mvi.MviUiState

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) : MviUiState

sealed interface LoginUiEvent : MviUiEvent {
    data class UsernameChanged(val v: String) : LoginUiEvent
    data class PasswordChanged(val v: String) : LoginUiEvent
    data object PasswordVisibilityToggled : LoginUiEvent
    data object LoginClicked : LoginUiEvent
    data object ErrorDismissed : LoginUiEvent
}

sealed interface LoginUiEffect : MviUiEffect {
    data object NavigateToTasks : LoginUiEffect
}
