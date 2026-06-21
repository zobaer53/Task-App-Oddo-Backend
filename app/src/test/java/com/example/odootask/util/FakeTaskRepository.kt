package com.example.odootask.util

import com.example.odootask.domain.model.Task
import com.example.odootask.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hand-written [TaskRepository] fake. [observeTasks] is backed by a [MutableStateFlow] so tests
 * can emit updates (Room-as-source-of-truth behaviour); the suspend calls return configurable
 * Results and record their arguments.
 */
class FakeTaskRepository : TaskRepository {

    private val tasks = MutableStateFlow<List<Task>>(emptyList())

    var getTasksResult: Result<List<Task>> = Result.success(emptyList())
    var createTaskResult: Result<Int> = Result.success(42)
    var updateStageResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)
    var getTaskHandler: (Int) -> Task? = { id -> tasks.value.firstOrNull { it.id == id } }

    var getTasksCount = 0
    var lastCreate: CreateArgs? = null
    var lastUpdateStage: Pair<Int, Int>? = null
    var lastDeletedTaskId: Int? = null

    data class CreateArgs(
        val name: String,
        val projectId: Int,
        val description: String?,
        val dateDeadline: String?,
        val userIds: List<Int>,
    )

    override fun observeTasks() = tasks.asStateFlow()

    override suspend fun getTasks(): Result<List<Task>> {
        getTasksCount++
        return getTasksResult.onSuccess { tasks.value = it }
    }

    override suspend fun getTask(taskId: Int): Task? = getTaskHandler(taskId)

    override suspend fun createTask(
        name: String,
        projectId: Int,
        description: String?,
        dateDeadline: String?,
        userIds: List<Int>,
    ): Result<Int> {
        lastCreate = CreateArgs(name, projectId, description, dateDeadline, userIds)
        return createTaskResult
    }

    override suspend fun updateTaskStage(taskId: Int, stageId: Int): Result<Unit> {
        lastUpdateStage = taskId to stageId
        return updateStageResult
    }

    override suspend fun deleteTask(taskId: Int): Result<Unit> {
        lastDeletedTaskId = taskId
        return deleteResult
    }

    /** Push a new task list onto the observed Flow, as a Room write-through would. */
    fun emitTasks(list: List<Task>) {
        tasks.value = list
    }
}
