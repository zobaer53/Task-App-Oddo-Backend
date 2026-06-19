package com.example.odootask.domain.usecase.task

import com.example.odootask.domain.repository.TaskRepository
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(
        name: String,
        projectId: Int,
        dateDeadline: String?,
        userIds: List<Int>,
    ): Result<Int> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Task name is required"))
        }
        if (projectId <= 0) {
            return Result.failure(IllegalArgumentException("A project must be selected"))
        }
        return repository.createTask(name.trim(), projectId, dateDeadline, userIds)
    }
}
