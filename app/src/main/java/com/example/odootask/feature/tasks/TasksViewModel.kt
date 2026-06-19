package com.example.odootask.feature.tasks

import androidx.lifecycle.viewModelScope
import com.example.odootask.core.mvi.MviViewModel
import com.example.odootask.domain.usecase.auth.GetSessionUseCase
import com.example.odootask.domain.usecase.auth.LogoutUseCase
import com.example.odootask.domain.usecase.task.GetTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getSessionUseCase: GetSessionUseCase,
) : MviViewModel<TasksUiState, TasksUiEvent, TasksUiEffect>(TasksUiState()) {

    private var hasLoaded = false

    override fun onEvent(event: TasksUiEvent) {
        when (event) {
            TasksUiEvent.Appeared -> onAppeared()
            TasksUiEvent.RefreshPulled -> loadTasks(isRefresh = true)
            is TasksUiEvent.TaskClicked -> sendEffect(TasksUiEffect.NavigateToTaskDetail(event.taskId))
            TasksUiEvent.CreateTaskClicked -> sendEffect(TasksUiEffect.NavigateToCreateTask)
            TasksUiEvent.LogoutClicked -> logout()
            TasksUiEvent.ErrorDismissed -> updateState { copy(errorMessage = null) }
        }
    }

    /** Greeting is refreshed every time; the task list loads only once per ViewModel. */
    private fun onAppeared() {
        loadUsername()
        if (hasLoaded) return
        hasLoaded = true
        loadTasks(isRefresh = false)
    }

    private fun loadUsername() {
        viewModelScope.launch {
            getSessionUseCase().first()?.let { user ->
                updateState { copy(username = user.username) }
            }
        }
    }

    private fun loadTasks(isRefresh: Boolean) {
        if (state.value.isLoading || state.value.isRefreshing) return
        updateState {
            if (isRefresh) copy(isRefreshing = true, errorMessage = null)
            else copy(isLoading = true, errorMessage = null)
        }
        viewModelScope.launch {
            getTasksUseCase().fold(
                onSuccess = { tasks ->
                    updateState { copy(tasks = tasks, isLoading = false, isRefreshing = false) }
                },
                onFailure = { e ->
                    updateState { copy(isLoading = false, isRefreshing = false) }
                    sendEffect(TasksUiEffect.ShowSnackbar(e.message ?: "Failed to load tasks"))
                },
            )
        }
    }

    private fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            sendEffect(TasksUiEffect.NavigateToLogin)
        }
    }
}
