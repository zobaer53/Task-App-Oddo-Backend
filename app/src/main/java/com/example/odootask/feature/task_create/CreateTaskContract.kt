package com.example.odootask.feature.task_create

import com.example.odootask.core.mvi.MviUiEffect
import com.example.odootask.core.mvi.MviUiEvent
import com.example.odootask.core.mvi.MviUiState

data class CreateTaskUiState(
    val name: String = "",
    val description: String = "",
    /** Odoo deadline in `yyyy-MM-dd`; empty means none was picked. */
    val deadline: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) : MviUiState {
    /** Allow saving only once a title is present and no write is in flight. */
    val canCreate: Boolean
        get() = name.isNotBlank() && !isSaving
}

sealed interface CreateTaskUiEvent : MviUiEvent {
    data object Appeared : CreateTaskUiEvent
    data class NameChanged(val v: String) : CreateTaskUiEvent
    data class DescriptionChanged(val v: String) : CreateTaskUiEvent
    data class DeadlineSelected(val isoDate: String) : CreateTaskUiEvent
    data object CreateClicked : CreateTaskUiEvent
    data object BackClicked : CreateTaskUiEvent
    data object ErrorDismissed : CreateTaskUiEvent
}

sealed interface CreateTaskUiEffect : MviUiEffect {
    data object NavigateBack : CreateTaskUiEffect
    data class ShowSnackbar(val message: String) : CreateTaskUiEffect
}
