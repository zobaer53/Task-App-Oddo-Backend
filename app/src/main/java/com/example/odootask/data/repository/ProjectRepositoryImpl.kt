package com.example.odootask.data.repository

import com.example.odootask.data.local.prefs.SessionData
import com.example.odootask.data.local.prefs.SessionDataStore
import com.example.odootask.data.remote.OdooApiService
import com.example.odootask.data.remote.dto.ObjectParams
import com.example.odootask.data.remote.dto.OdooRequest
import com.example.odootask.data.remote.dto.ProjectDto
import com.example.odootask.data.remote.dto.StageDto
import com.example.odootask.domain.model.Project
import com.example.odootask.domain.model.Stage
import com.example.odootask.domain.repository.ProjectRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies the projects and stages needed by the create-task and task-detail
 * screens via the Odoo `object` service. Both are short, rarely-changing lists,
 * so they are fetched on demand rather than cached.
 */
@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val api: OdooApiService,
    private val sessionDataStore: SessionDataStore,
    private val gson: Gson,
) : ProjectRepository {

    override suspend fun getProjects(): Result<List<Project>> = runCatching {
        val s = requireSession()
        val args = listOf(
            s.database, s.uid, s.password,
            "project.project", "search_read",
            listOf(emptyList<Any>()), // positional args: [domain]
            mapOf("fields" to listOf("id", "name")), // kwargs
        )
        val resp = api.callJsonRpc(OdooRequest(params = ObjectParams(args = args)))
        val el = resp.result ?: throw RuntimeException(resp.error?.data?.message ?: "Failed to load projects")

        gson.fromJson(el, Array<ProjectDto>::class.java).map { Project(id = it.id, name = it.name) }
    }

    override suspend fun getStages(): Result<List<Stage>> = runCatching {
        val s = requireSession()
        // Exclude per-user personal stages (Inbox/Today/This Week/…); those carry a
        // user_id, while the shared project stages we want have user_id = false.
        val domain = listOf(listOf("user_id", "=", false))
        val args = listOf(
            s.database, s.uid, s.password,
            "project.task.type", "search_read",
            listOf(domain), // positional args: [domain]
            mapOf("fields" to listOf("id", "name")), // kwargs
        )
        val resp = api.callJsonRpc(OdooRequest(params = ObjectParams(args = args)))
        val el = resp.result ?: throw RuntimeException(resp.error?.data?.message ?: "Failed to load stages")

        gson.fromJson(el, Array<StageDto>::class.java).map { Stage(id = it.id, name = it.name) }
    }

    private suspend fun requireSession(): SessionData =
        sessionDataStore.getSession().first() ?: throw IllegalStateException("Not logged in")
}
