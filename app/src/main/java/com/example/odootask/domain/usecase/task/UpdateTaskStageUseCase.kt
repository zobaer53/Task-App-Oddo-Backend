package com.example.odootask.domain.usecase.task

import com.example.odootask.domain.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskStageUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Int, stageId: Int): Result<Unit> {
        if (taskId <= 0) {
            return Result.failure(IllegalArgumentException("Invalid task"))
        }
        if (stageId <= 0) {
            return Result.failure(IllegalArgumentException("A stage must be selected"))
        }
        return repository.updateTaskStage(taskId, stageId)
    }
}
