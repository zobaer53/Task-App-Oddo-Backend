package com.example.odootask.data.repository

import com.example.odootask.data.local.dao.TaskDao
import com.example.odootask.data.local.entity.toDomain
import com.example.odootask.data.local.prefs.SessionData
import com.example.odootask.data.local.prefs.SessionDataStore
import com.example.odootask.data.remote.OdooApiService
import com.example.odootask.data.remote.dto.ObjectParams
import com.example.odootask.data.remote.dto.OdooRequest
import com.example.odootask.data.remote.dto.TaskDto
import com.example.odootask.data.remote.dto.toEntity
import com.example.odootask.domain.model.Task
import com.example.odootask.domain.repository.TaskRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and writes `project.task` records through the Odoo `object` service.
 * Every call replays the persisted database/uid/password from [SessionDataStore].
 * Reads also upsert into [TaskDao] so the list survives offline and process death;
 * the raw [com.google.gson.JsonElement] result is decoded per-call with [gson].
 */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val api: OdooApiService,
    private val taskDao: TaskDao,
    private val sessionDataStore: SessionDataStore,
    private val gson: Gson,
) : TaskRepository {

    private val taskFields = listOf("id", "name", "stage_id", "date_deadline")

    override suspend fun getTasks(): Result<List<Task>> = runCatching {
        val s = requireSession()
        val args = listOf(
            s.database, s.uid, s.password,
            "project.task", "search_read",
            listOf(emptyList<Any>()), // positional args: [domain]
            mapOf("fields" to taskFields, "limit" to 80), // kwargs
        )
        val resp = api.callJsonRpc(OdooRequest(params = ObjectParams(args = args)))
        val el = resp.result ?: throw RuntimeException(resp.error?.data?.message ?: "Failed to load tasks")

        val entities = gson.fromJson(el, Array<TaskDto>::class.java).map { it.toEntity() }
        taskDao.clear()
        taskDao.upsertAll(entities)
        entities.map { it.toDomain() }
    }

    override suspend fun createTask(
        name: String,
        projectId: Int,
        dateDeadline: String?,
        userIds: List<Int>,
    ): Result<Int> = runCatching {
        val s = requireSession()
        val values = buildMap<String, Any> {
            put("name", name)
            put("project_id", projectId)
            if (!dateDeadline.isNullOrBlank()) put("date_deadline", dateDeadline)
            // many2many command 4 = link an existing record (assign these users)
            if (userIds.isNotEmpty()) put("user_ids", userIds.map { listOf(4, it) })
        }
        val args = listOf(s.database, s.uid, s.password, "project.task", "create", listOf(values))
        val resp = api.callJsonRpc(OdooRequest(params = ObjectParams(args = args)))
        val el = resp.result ?: throw RuntimeException(resp.error?.data?.message ?: "Failed to create task")

        if (el.isJsonPrimitive && el.asJsonPrimitive.isNumber) el.asInt else 0
    }

    override suspend fun updateTaskStage(taskId: Int, stageId: Int): Result<Unit> = runCatching {
        val s = requireSession()
        val writeArgs = listOf(
            s.database, s.uid, s.password,
            "project.task", "write",
            listOf(listOf(taskId), mapOf("stage_id" to stageId)),
        )
        val resp = api.callJsonRpc(OdooRequest(params = ObjectParams(args = writeArgs)))
        resp.result ?: throw RuntimeException(resp.error?.data?.message ?: "Failed to update stage")

        // Re-read the row so the cached stage name reflects the new stage.
        val readArgs = listOf(
            s.database, s.uid, s.password,
            "project.task", "read",
            listOf(listOf(taskId)), // positional args: [ids]
            mapOf("fields" to taskFields), // kwargs
        )
        val readResp = api.callJsonRpc(OdooRequest(params = ObjectParams(args = readArgs)))
        readResp.result?.let { el ->
            gson.fromJson(el, Array<TaskDto>::class.java)
                .map { it.toEntity() }
                .takeIf { it.isNotEmpty() }
                ?.let { taskDao.upsertAll(it) }
        }
        Unit
    }

    private suspend fun requireSession(): SessionData =
        sessionDataStore.getSession().first() ?: throw IllegalStateException("Not logged in")
}
