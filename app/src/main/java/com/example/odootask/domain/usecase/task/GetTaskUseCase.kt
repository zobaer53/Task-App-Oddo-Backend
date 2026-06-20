package com.example.odootask.domain.usecase.task

import com.example.odootask.domain.model.Task
import com.example.odootask.domain.repository.TaskRepository
import javax.inject.Inject

class GetTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Int): Task? = repository.getTask(taskId)
}
