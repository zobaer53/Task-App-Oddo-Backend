package com.example.odootask.feature.task_detail

import com.example.odootask.core.mvi.MviUiEffect
import com.example.odootask.core.mvi.MviUiEvent
import com.example.odootask.core.mvi.MviUiState
import com.example.odootask.domain.model.Stage
import com.example.odootask.domain.model.Task

data class TaskDetailUiState(
    val task: Task? = null,
    val stages: List<Stage> = emptyList(),
    val selectedStage: Stage? = null,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
) : MviUiState {
    /** Enable the action only once a different stage is picked and no write is in flight. */
    val canUpdate: Boolean
        get() = task != null && selectedStage != null &&
            selectedStage.id != task.stageId && !isUpdating
}

sealed interface TaskDetailUiEvent : MviUiEvent {
    data object Appeared : TaskDetailUiEvent
    data class StageSelected(val stage: Stage) : TaskDetailUiEvent
    data object UpdateClicked : TaskDetailUiEvent
    data object BackClicked : TaskDetailUiEvent
    data object ErrorDismissed : TaskDetailUiEvent
}

sealed interface TaskDetailUiEffect : MviUiEffect {
    data object NavigateBack : TaskDetailUiEffect
    data class ShowSnackbar(val message: String) : TaskDetailUiEffect
}
