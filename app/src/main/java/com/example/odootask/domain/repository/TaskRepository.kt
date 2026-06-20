package com.example.odootask.domain.repository

import com.example.odootask.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun getTasks(): Result<List<Task>>
    suspend fun getTask(taskId: Int): Task?
    suspend fun createTask(
        name: String,
        projectId: Int,
        description: String?,
        dateDeadline: String?,
        userIds: List<Int>,
    ): Result<Int>
    suspend fun updateTaskStage(taskId: Int, stageId: Int): Result<Unit>
}
