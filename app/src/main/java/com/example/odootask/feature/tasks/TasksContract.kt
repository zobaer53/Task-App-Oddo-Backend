package com.example.odootask.feature.tasks

import com.example.odootask.core.mvi.MviUiEffect
import com.example.odootask.core.mvi.MviUiEvent
import com.example.odootask.core.mvi.MviUiState
import com.example.odootask.domain.model.Task

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val username: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
) : MviUiState

sealed interface TasksUiEvent : MviUiEvent {
    data object Appeared : TasksUiEvent
    data object RefreshPulled : TasksUiEvent
    data class TaskClicked(val taskId: Int) : TasksUiEvent
    data object CreateTaskClicked : TasksUiEvent
    data object LogoutClicked : TasksUiEvent
    data object ErrorDismissed : TasksUiEvent
}

sealed interface TasksUiEffect : MviUiEffect {
    data class NavigateToTaskDetail(val taskId: Int) : TasksUiEffect
    data object NavigateToCreateTask : TasksUiEffect
    data object NavigateToLogin : TasksUiEffect
    data class ShowSnackbar(val message: String) : TasksUiEffect
}
