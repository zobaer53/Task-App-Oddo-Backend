package com.example.odootask.feature.task_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.odootask.core.mvi.MviViewModel
import com.example.odootask.domain.model.Stage
import com.example.odootask.domain.usecase.task.GetStagesUseCase
import com.example.odootask.domain.usecase.task.GetTaskUseCase
import com.example.odootask.domain.usecase.task.UpdateTaskStageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val getTaskUseCase: GetTaskUseCase,
    private val getStagesUseCase: GetStagesUseCase,
    private val updateTaskStageUseCase: UpdateTaskStageUseCase,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<TaskDetailUiState, TaskDetailUiEvent, TaskDetailUiEffect>(TaskDetailUiState()) {

    private val taskId: Int = savedStateHandle.get<Int>("taskId") ?: 0
    private var hasLoaded = false

    override fun onEvent(event: TaskDetailUiEvent) {
        when (event) {
            TaskDetailUiEvent.Appeared -> onAppeared()
            is TaskDetailUiEvent.StageSelected -> updateState { copy(selectedStage = event.stage) }
            TaskDetailUiEvent.UpdateClicked -> updateStage()
            TaskDetailUiEvent.BackClicked -> sendEffect(TaskDetailUiEffect.NavigateBack)
            TaskDetailUiEvent.ErrorDismissed -> updateState { copy(errorMessage = null) }
        }
    }

    private fun onAppeared() {
        if (hasLoaded) return
        hasLoaded = true
        loadDetail()
    }

    private fun loadDetail() {
        updateState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val task = getTaskUseCase(taskId)
            if (task == null) {
                updateState { copy(isLoading = false, errorMessage = "Task not found") }
                return@launch
            }
            val stages = getStagesUseCase().getOrElse { emptyList() }
            // Pre-select the task's current stage; fall back to its denormalised name.
            val current = stages.firstOrNull { it.id == task.stageId }
                ?: Stage(task.stageId, task.stageName)
            updateState {
                copy(
                    task = task,
                    stages = stages,
                    selectedStage = current,
                    isLoading = false,
                )
            }
        }
    }

    private fun updateStage() {
        val state = state.value
        val task = state.task ?: return
        val stage = state.selectedStage ?: return
        if (state.isUpdating) return

        updateState { copy(isUpdating = true, errorMessage = null) }
        viewModelScope.launch {
            updateTaskStageUseCase(task.id, stage.id).fold(
                onSuccess = {
                    // The write goes through to the Room cache, so the task list row
                    // refreshes on its own once we pop back to it.
                    updateState { copy(isUpdating = false) }
                    sendEffect(TaskDetailUiEffect.NavigateBack)
                },
                onFailure = { e ->
                    updateState { copy(isUpdating = false) }
                    sendEffect(TaskDetailUiEffect.ShowSnackbar(e.message ?: "Failed to update status"))
                },
            )
        }
    }
}
