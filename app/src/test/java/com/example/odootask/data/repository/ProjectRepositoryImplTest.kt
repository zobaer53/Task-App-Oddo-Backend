package com.example.odootask.data.repository

import com.example.odootask.data.local.prefs.SessionData
import com.example.odootask.data.local.prefs.SessionDataStore
import com.example.odootask.data.remote.OdooApiService
import com.example.odootask.data.remote.dto.ObjectParams
import com.example.odootask.data.remote.dto.OdooRequest
import com.example.odootask.util.jsonResult
import com.example.odootask.util.rpcError
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Verifies [ProjectRepositoryImpl] builds the `search_read` args for projects and stages —
 * including the `user_id = false` domain that excludes Odoo's per-user personal stages
 * (phase-10 fix) — and surfaces the not-logged-in / error branches.
 */
class ProjectRepositoryImplTest {

    private val api: OdooApiService = mockk()
    private val sessionDataStore: SessionDataStore = mockk()
    private val gson = Gson()

    private val repo = ProjectRepositoryImpl(api, sessionDataStore, gson)

    private val session = SessionData(database = "zobaer", username = "admin", password = "secret", uid = 7)

    @Test
    fun `getProjects builds search_read args and maps records`() = runTest {
        every { sessionDataStore.getSession() } returns flowOf(session)
        val request = slot<OdooRequest>()
        coEvery { api.callJsonRpc(capture(request)) } returns
            jsonResult("""[{"id":1,"name":"Internal"},{"id":2,"name":"Website"}]""")

        val result = repo.getProjects()

        val projects = result.getOrNull()!!
        assertThat(projects.map { it.name }).containsExactly("Internal", "Website").inOrder()

        val args = (request.captured.params as ObjectParams).args
        assertThat(args.subList(0, 5))
            .containsExactly("zobaer", 7, "secret", "project.project", "search_read").inOrder()
        assertThat(args[5]).isEqualTo(listOf(emptyList<Any>()))
        assertThat(args[6]).isEqualTo(mapOf("fields" to listOf("id", "name")))
    }

    @Test
    fun `getStages excludes personal stages via user_id false domain`() = runTest {
        every { sessionDataStore.getSession() } returns flowOf(session)
        val request = slot<OdooRequest>()
        coEvery { api.callJsonRpc(capture(request)) } returns
            jsonResult("""[{"id":1,"name":"To Do"},{"id":2,"name":"Done"}]""")

        val result = repo.getStages()

        assertThat(result.getOrNull()!!.map { it.name }).containsExactly("To Do", "Done").inOrder()

        val args = (request.captured.params as ObjectParams).args
        assertThat(args.subList(0, 5))
            .containsExactly("zobaer", 7, "secret", "project.task.type", "search_read").inOrder()
        assertThat(args[5]).isEqualTo(listOf(listOf(listOf("user_id", "=", false))))
        assertThat(args[6]).isEqualTo(mapOf("fields" to listOf("id", "name")))
    }

    @Test
    fun `getProjects without a session fails`() = runTest {
        every { sessionDataStore.getSession() } returns flowOf(null)

        val result = repo.getProjects()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `getStages surfaces the rpc error message`() = runTest {
        every { sessionDataStore.getSession() } returns flowOf(session)
        coEvery { api.callJsonRpc(any()) } returns rpcError("stage load failed")

        val result = repo.getStages()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()!!.message).isEqualTo("stage load failed")
    }
}
