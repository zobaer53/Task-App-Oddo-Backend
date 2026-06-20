package com.example.odootask.feature.account

import com.example.odootask.core.mvi.MviUiEffect
import com.example.odootask.core.mvi.MviUiEvent
import com.example.odootask.core.mvi.MviUiState

data class UpdateAccountUiState(
    val name: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) : MviUiState {
    /** Allow saving only once a name is present and no read/write is in flight. */
    val canSave: Boolean
        get() = name.isNotBlank() && !isLoading && !isSaving
}

sealed interface UpdateAccountUiEvent : MviUiEvent {
    data object Appeared : UpdateAccountUiEvent
    data class NameChanged(val v: String) : UpdateAccountUiEvent
    data object SaveClicked : UpdateAccountUiEvent
    data object BackClicked : UpdateAccountUiEvent
    data object ErrorDismissed : UpdateAccountUiEvent
}

sealed interface UpdateAccountUiEffect : MviUiEffect {
    data object NavigateBack : UpdateAccountUiEffect
    data class ShowSnackbar(val message: String) : UpdateAccountUiEffect
}
