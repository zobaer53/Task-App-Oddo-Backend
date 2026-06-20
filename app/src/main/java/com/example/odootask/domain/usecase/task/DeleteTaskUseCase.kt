package com.example.odootask.domain.usecase.task

import com.example.odootask.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Int): Result<Unit> {
        if (taskId <= 0) {
            return Result.failure(IllegalArgumentException("Invalid task"))
        }
        return repository.deleteTask(taskId)
    }
}
