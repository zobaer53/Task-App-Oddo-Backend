package com.example.odootask.data.repository

import com.example.odootask.data.local.dao.TaskDao
import com.example.odootask.data.local.prefs.SessionData
import com.example.odootask.data.local.prefs.SessionDataStore
import com.example.odootask.data.remote.OdooApiService
import com.example.odootask.data.remote.dto.CommonParams
import com.example.odootask.data.remote.dto.DbParams
import com.example.odootask.data.remote.dto.ObjectParams
import com.example.odootask.data.remote.dto.OdooRequest
import com.example.odootask.util.jsonResult
import com.example.odootask.util.rpcError
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Verifies [AuthRepositoryImpl] builds the `common.authenticate` and `object`-service argument
 * lists in the exact order Odoo expects, parses the uid/result, and surfaces the error branch.
 */
class AuthRepositoryImplTest {

    private val api: OdooApiService = mockk()
    private val sessionDataStore: SessionDataStore = mockk(relaxed = true)
    private val taskDao: TaskDao = mockk(relaxed = true)

    private val repo = AuthRepositoryImpl(api, sessionDataStore, taskDao)

    private val session = SessionData(database = "zobaer", username = "admin", password = "secret", uid = 7)

    @Test
    fun `login resolves database, sends authenticate args in order, and saves session`() = runTest {
        val requests = mutableListOf<OdooRequest>()
        coEvery { api.callJsonRpc(capture(requests)) } answers {
            when (firstArg<OdooRequest>().params) {
                is DbParams -> jsonResult("""["zobaer"]""")
                is CommonParams -> jsonResult("7")
                else -> jsonResult("false")
            }
        }
        val saved = slot<SessionData>()
        coEvery { sessionDataStore.saveSession(capture(saved)) } returns Unit

        val result = repo.login(username = "admin", password = "secret")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()!!.uid).isEqualTo(7)

        val authArgs = requests.map { it.params }.filterIsInstance<CommonParams>().single().args
        assertThat(authArgs).hasSize(4)
        assertThat(authArgs[0]).isEqualTo("zobaer")
        assertThat(authArgs[1]).isEqualTo("admin")
        assertThat(authArgs[2]).isEqualTo("secret")
        assertThat(authArgs[3]).isEqualTo(emptyMap<String, Any>())

        assertThat(saved.captured).isEqualTo(session)
    }

    @Test
    fun `login with uid zero fails and does not save a session`() = runTest {
        coEvery { api.callJsonRpc(any()) } answers {
            when (firstArg<OdooRequest>().params) {
                is DbParams -> jsonResult("""["zobaer"]""")
                else -> jsonResult("false") // authenticate rejected → uid 0
            }
        }

        val result = repo.login(username = "admin", password = "wrong")

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { sessionDataStore.saveSession(any()) }
    }

    @Test
    fun `login surfaces the rpc error message`() = runTest {
        coEvery { api.callJsonRpc(any()) } answers {
            when (firstArg<OdooRequest>().params) {
                is DbParams -> jsonResult("""["zobaer"]""")
                else -> rpcError("Access Denied")
            }
        }

        val result = repo.login(username = "admin", password = "secret")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()!!.message).isEqualTo("Access Denied")
    }

    @Test
    fun `getAccountName builds res_users read args and parses name`() = runTest {
        every { sessionDataStore.getSession() } returns flowOf(session)
        val request = slot<OdooRequest>()
        coEvery { api.callJsonRpc(capture(request)) } returns jsonResult("""[{"id":7,"name":"Administrator"}]""")

        val result = repo.getAccountName()

        assertThat(result.getOrNull()).isEqualTo("Administrator")
        val args = (request.captured.params as ObjectParams).args
        assertThat(args.subList(0, 5)).containsExactly("zobaer", 7, "secret", "res.users", "read").inOrder()
        assertThat(args[5]).isEqualTo(listOf(listOf(7)))
        assertThat(args[6]).isEqualTo(mapOf("fields" to listOf("name")))
    }

    @Test
    fun `updateAccountName builds res_users write args`() = runTest {
        every { sessionDataStore.getSession() } returns flowOf(session)
        val request = slot<OdooRequest>()
        coEvery { api.callJsonRpc(capture(request)) } returns jsonResult("true")

        val result = repo.updateAccountName("New Name")

        assertThat(result.isSuccess).isTrue()
        val args = (request.captured.params as ObjectParams).args
        assertThat(args.subList(0, 5)).containsExactly("zobaer", 7, "secret", "res.users", "write").inOrder()
        assertThat(args[5]).isEqualTo(listOf(listOf(7), mapOf("name" to "New Name")))
    }

    @Test
    fun `logout clears the session and the task cache`() = runTest {
        repo.logout()

        coVerify { sessionDataStore.clearSession() }
        coVerify { taskDao.clear() }
    }
}
