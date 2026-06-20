package com.example.odootask.feature.tasks

import androidx.lifecycle.viewModelScope
import com.example.odootask.core.mvi.MviViewModel
import com.example.odootask.domain.usecase.auth.GetAccountNameUseCase
import com.example.odootask.domain.usecase.auth.GetSessionUseCase
import com.example.odootask.domain.usecase.auth.LogoutUseCase
import com.example.odootask.domain.usecase.task.DeleteTaskUseCase
import com.example.odootask.domain.usecase.task.GetTasksUseCase
import com.example.odootask.domain.usecase.task.ObserveTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    observeTasksUseCase: ObserveTasksUseCase,
    private val getTasksUseCase: GetTasksUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getSessionUseCase: GetSessionUseCase,
    private val getAccountNameUseCase: GetAccountNameUseCase,
) : MviViewModel<TasksUiState, TasksUiEvent, TasksUiEffect>(TasksUiState()) {

    private var hasLoaded = false

    init {
        // The cache is the single source of truth: a stage update from the detail
        // screen writes through to Room, so the matching row refreshes here on return.
        observeTasksUseCase()
            .onEach { tasks -> updateState { copy(tasks = tasks) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: TasksUiEvent) {
        when (event) {
            TasksUiEvent.Appeared -> onAppeared()
            TasksUiEvent.RefreshPulled -> loadTasks(isRefresh = true)
            is TasksUiEvent.TaskClicked -> sendEffect(TasksUiEffect.NavigateToTaskDetail(event.taskId))
            is TasksUiEvent.TaskDeleted -> deleteTask(event.taskId)
            TasksUiEvent.CreateTaskClicked -> sendEffect(TasksUiEffect.NavigateToCreateTask)
            TasksUiEvent.UpdateAccountClicked -> sendEffect(TasksUiEffect.NavigateToUpdateAccount)
            TasksUiEvent.LogoutClicked -> logout()
            TasksUiEvent.ErrorDismissed -> updateState { copy(errorMessage = null) }
        }
    }

    /** Greeting is refreshed every time; the task list loads only once per ViewModel. */
    private fun onAppeared() {
        loadAccount()
        if (hasLoaded) return
        hasLoaded = true
        loadTasks(isRefresh = false)
    }

    private fun loadAccount() {
        viewModelScope.launch {
            // The session login doubles as the user's email; the display name comes
            // from the live res.users record so it reflects any account edits.
            getSessionUseCase().first()?.let { user ->
                updateState { copy(email = user.username) }
            }
            getAccountNameUseCase().onSuccess { name ->
                updateState { copy(name = name) }
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
            // Success updates the Room cache, which the observer above reflects into state.
            getTasksUseCase().fold(
                onSuccess = {
                    updateState { copy(isLoading = false, isRefreshing = false) }
                },
                onFailure = { e ->
                    updateState { copy(isLoading = false, isRefreshing = false) }
                    sendEffect(TasksUiEffect.ShowSnackbar(e.message ?: "Failed to load tasks"))
                },
            )
        }
    }

    private fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            // The cache row is removed on success, which the observer reflects into state.
            deleteTaskUseCase(taskId).fold(
                onSuccess = { sendEffect(TasksUiEffect.ShowSnackbar("Task deleted")) },
                onFailure = { e ->
                    sendEffect(TasksUiEffect.ShowSnackbar(e.message ?: "Failed to delete task"))
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
