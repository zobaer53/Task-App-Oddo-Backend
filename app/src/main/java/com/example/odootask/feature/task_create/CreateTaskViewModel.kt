package com.example.odootask.feature.task_create

import androidx.lifecycle.viewModelScope
import com.example.odootask.core.mvi.MviViewModel
import com.example.odootask.domain.model.Project
import com.example.odootask.domain.usecase.auth.GetSessionUseCase
import com.example.odootask.domain.usecase.task.CreateTaskUseCase
import com.example.odootask.domain.usecase.task.GetProjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val createTaskUseCase: CreateTaskUseCase,
    private val getProjectsUseCase: GetProjectsUseCase,
    private val getSessionUseCase: GetSessionUseCase,
) : MviViewModel<CreateTaskUiState, CreateTaskUiEvent, CreateTaskUiEffect>(CreateTaskUiState()) {

    // The reference UI exposes no project picker, so the new task lands in the first
    // available project; both are resolved silently when the screen appears.
    private var targetProject: Project? = null
    private var currentUid: Int = 0
    private var hasLoaded = false

    override fun onEvent(event: CreateTaskUiEvent) {
        when (event) {
            CreateTaskUiEvent.Appeared -> onAppeared()
            is CreateTaskUiEvent.NameChanged -> updateState { copy(name = event.v) }
            is CreateTaskUiEvent.DescriptionChanged -> updateState { copy(description = event.v) }
            is CreateTaskUiEvent.DeadlineSelected -> updateState { copy(deadline = event.isoDate) }
            CreateTaskUiEvent.CreateClicked -> createTask()
            CreateTaskUiEvent.BackClicked -> sendEffect(CreateTaskUiEffect.NavigateBack)
            CreateTaskUiEvent.ErrorDismissed -> updateState { copy(errorMessage = null) }
        }
    }

    private fun onAppeared() {
        if (hasLoaded) return
        hasLoaded = true
        viewModelScope.launch {
            currentUid = getSessionUseCase().first()?.uid ?: 0
            targetProject = getProjectsUseCase().getOrNull()?.firstOrNull()
        }
    }

    private fun createTask() {
        val state = state.value
        if (!state.canCreate) return

        val project = targetProject
        if (project == null) {
            sendEffect(CreateTaskUiEffect.ShowSnackbar("No project available to create the task in"))
            return
        }

        updateState { copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            createTaskUseCase(
                name = state.name,
                projectId = project.id,
                description = state.description,
                dateDeadline = state.deadline.ifBlank { null },
                userIds = if (currentUid > 0) listOf(currentUid) else emptyList(),
            ).fold(
                onSuccess = {
                    updateState { copy(isSaving = false) }
                    sendEffect(CreateTaskUiEffect.ShowSnackbar("Task created"))
                    sendEffect(CreateTaskUiEffect.NavigateBack)
                },
                onFailure = { e ->
                    updateState { copy(isSaving = false) }
                    sendEffect(CreateTaskUiEffect.ShowSnackbar(e.message ?: "Failed to create task"))
                },
            )
        }
    }
}
