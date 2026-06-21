package com.example.odootask.data.repository

import com.example.odootask.data.local.dao.TaskDao
import com.example.odootask.data.local.entity.TaskEntity
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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Verifies [TaskRepositoryImpl] builds correct `execute_kw` argument lists for each
 * `project.task` operation, decodes the JSON-RPC result, writes through to [TaskDao], and
 * surfaces the not-logged-in and error branches as failures.
 */
class TaskRepositoryImplTest {

    private val api: OdooApiService = mockk()
    private val taskDao: TaskDao = mockk(relaxed = true)
    private val sessionDataStore: SessionDataStore = mockk()
    private val gson = Gson()

    private val repo = TaskRepositoryImpl(api, taskDao, sessionDataStore, gson)

    private val session = SessionData(database = "zobaer", username = "admin", password = "secret", uid = 7)

    private fun loggedIn() {
        every { sessionDataStore.getSession() } returns flowOf(session)
    }

    private fun loggedOut() {
        every { sessionDataStore.getSession() } returns flowOf(null)
    }

    @Test
    fun `getTasks builds search_read args, maps records, and replaces the cache`() = runTest {
        loggedIn()
        val request = slot<OdooRequest>()
        coEvery { api.callJsonRpc(capture(request)) } returns
            jsonResult("""[{"id":1,"name":"A","stage_id":[4,"In Progress"],"date_deadline":"2026-07-01"}]""")
        val cached = slot<List<TaskEntity>>()
        coEvery { taskDao.replaceAll(capture(cached)) } returns Unit

        val result = repo.getTasks()

        val tasks = result.getOrNull()!!
        assertThat(tasks).hasSize(1)
        assertThat(tasks[0].stageId).isEqualTo(4)
        assertThat(tasks[0].stageName).isEqualTo("In Progress")

        val args = (request.captured.params as ObjectParams).args
        assertThat(args.subList(0, 5))
            .containsExactly("zobaer", 7, "secret", "project.task", "search_read").inOrder()
        assertThat(args[5]).isEqualTo(listOf(emptyList<Any>()))
        assertThat(args[6]).isEqualTo(mapOf("fields" to listOf("id", "name", "stage_id", "date_deadline"), "limit" to 80))

        assertThat(cached.captured).hasSize(1)
        coVerify { taskDao.replaceAll(any()) }
    }

    @Test
    fun `getTasks without a session fails`() = runTest {
        loggedOut()

        val result = repo.getTasks()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `getTasks surfaces the rpc error message`() = runTest {
        loggedIn()
        coEvery { api.callJsonRpc(any()) } returns rpcError("boom")

        val result = repo.getTasks()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()!!.message).isEqualTo("boom")
    }

    @Test
    fun `createTask encodes user_ids as link commands and includes optional fields`() = runTest {
        loggedIn()
        val requests = mutableListOf<OdooRequest>()
        coEvery { api.callJsonRpc(capture(requests)) } answers {
            val method = (firstArg<OdooRequest>().params as ObjectParams).args[4] as String
            when (method) {
                "create" -> jsonResult("5")
                else -> jsonResult("""[{"id":5,"name":"New","stage_id":[1,"To Do"],"date_deadline":false}]""")
            }
        }

        val result = repo.createTask(
            name = "New",
            projectId = 3,
            description = "details",
            dateDeadline = "2026-07-01",
            userIds = listOf(7),
        )

        assertThat(result.getOrNull()).isEqualTo(5)

        val createArgs = requests.map { it.params as ObjectParams }.first { it.args[4] == "create" }.args
        assertThat(createArgs.subList(0, 5))
            .containsExactly("zobaer", 7, "secret", "project.task", "create").inOrder()
        @Suppress("UNCHECKED_CAST")
        val values = (createArgs[5] as List<*>).first() as Map<String, Any>
        assertThat(values["name"]).isEqualTo("New")
        assertThat(values["project_id"]).isEqualTo(3)
        assertThat(values["description"]).isEqualTo("details")
        assertThat(values["date_deadline"]).isEqualTo("2026-07-01")
        assertThat(values["user_ids"]).isEqualTo(listOf(listOf(4, 7)))

        // The created row is read back and cached so the reactive list shows it without a refresh.
        coVerify { taskDao.upsertAll(any()) }
    }

    @Test
    fun `createTask omits blank optional fields and empty user list`() = runTest {
        loggedIn()
        val requests = mutableListOf<OdooRequest>()
        coEvery { api.callJsonRpc(capture(requests)) } answers {
            val method = (firstArg<OdooRequest>().params as ObjectParams).args[4] as String
            if (method == "create") jsonResult("6") else jsonResult("[]")
        }

        repo.createTask(name = "Minimal", projectId = 3, description = "  ", dateDeadline = null, userIds = emptyList())

        val createArgs = requests.map { it.params as ObjectParams }.first { it.args[4] == "create" }.args
        @Suppress("UNCHECKED_CAST")
        val values = (createArgs[5] as List<*>).first() as Map<String, Any>
        assertThat(values).containsKey("name")
        assertThat(values).containsKey("project_id")
        assertThat(values).doesNotContainKey("description")
        assertThat(values).doesNotContainKey("date_deadline")
        assertThat(values).doesNotContainKey("user_ids")
    }

    @Test
    fun `updateTaskStage builds write args and re-reads the row`() = runTest {
        loggedIn()
        val requests = mutableListOf<OdooRequest>()
        coEvery { api.callJsonRpc(capture(requests)) } answers {
            val method = (firstArg<OdooRequest>().params as ObjectParams).args[4] as String
            if (method == "write") jsonResult("true")
            else jsonResult("""[{"id":5,"name":"T","stage_id":[2,"Done"],"date_deadline":false}]""")
        }

        val result = repo.updateTaskStage(taskId = 5, stageId = 2)

        assertThat(result.isSuccess).isTrue()
        val writeArgs = requests.map { it.params as ObjectParams }.first { it.args[4] == "write" }.args
        assertThat(writeArgs.subList(0, 5))
            .containsExactly("zobaer", 7, "secret", "project.task", "write").inOrder()
        assertThat(writeArgs[5]).isEqualTo(listOf(listOf(5), mapOf("stage_id" to 2)))
        coVerify { taskDao.upsertAll(any()) }
    }

    @Test
    fun `deleteTask builds unlink args and removes the cached row`() = runTest {
        loggedIn()
        val request = slot<OdooRequest>()
        coEvery { api.callJsonRpc(capture(request)) } returns jsonResult("true")

        val result = repo.deleteTask(taskId = 5)

        assertThat(result.isSuccess).isTrue()
        val args = (request.captured.params as ObjectParams).args
        assertThat(args.subList(0, 5))
            .containsExactly("zobaer", 7, "secret", "project.task", "unlink").inOrder()
        assertThat(args[5]).isEqualTo(listOf(listOf(5)))
        coVerify { taskDao.deleteById(5) }
    }

    @Test
    fun `observeTasks maps cached entities to domain`() = runTest {
        every { taskDao.observeAll() } returns
            flowOf(listOf(TaskEntity(id = 1, name = "A", stageId = 4, stageName = "In Progress", dateDeadline = null)))

        val tasks = repo.observeTasks().first()

        assertThat(tasks).hasSize(1)
        assertThat(tasks[0].name).isEqualTo("A")
    }

    @Test
    fun `getTask reads a single row from the cache`() = runTest {
        coEvery { taskDao.getById(5) } returns
            TaskEntity(id = 5, name = "Cached", stageId = 1, stageName = "To Do", dateDeadline = null)

        val task = repo.getTask(5)

        assertThat(task!!.name).isEqualTo("Cached")
    }
}
