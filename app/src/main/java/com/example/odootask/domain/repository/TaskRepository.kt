package com.example.odootask.domain.repository

import com.example.odootask.domain.model.Task

interface TaskRepository {
    suspend fun getTasks(): Result<List<Task>>
    suspend fun createTask(name: String, projectId: Int, dateDeadline: String?, userIds: List<Int>): Result<Int>
    suspend fun updateTaskStage(taskId: Int, stageId: Int): Result<Unit>
}
