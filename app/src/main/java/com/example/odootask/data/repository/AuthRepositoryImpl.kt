package com.example.odootask.data.repository

import com.example.odootask.data.local.dao.TaskDao
import com.example.odootask.data.local.prefs.SessionData
import com.example.odootask.data.local.prefs.SessionDataStore
import com.example.odootask.data.remote.OdooApiService
import com.example.odootask.data.remote.dto.CommonParams
import com.example.odootask.data.remote.dto.OdooRequest
import com.example.odootask.domain.model.OdooUser
import com.example.odootask.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authenticates against the Odoo `common` service. A successful `authenticate`
 * call returns the integer uid; an empty result (`false`) means the credentials
 * or database were wrong. The uid plus the supplied credentials are persisted in
 * [SessionDataStore] so later `object` calls can re-send them.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: OdooApiService,
    private val sessionDataStore: SessionDataStore,
    private val taskDao: TaskDao,
) : AuthRepository {

    override suspend fun login(
        database: String,
        username: String,
        password: String,
    ): Result<OdooUser> = runCatching {
        val args = listOf(database, username, password, emptyMap<String, Any>())
        val resp = api.callJsonRpc(OdooRequest(params = CommonParams(args = args)))
        resp.error?.let { throw RuntimeException(it.data?.message ?: it.message ?: "Authentication failed") }

        val result = resp.result
        val uid = if (result != null && result.isJsonPrimitive && result.asJsonPrimitive.isNumber) {
            result.asInt
        } else {
            0
        }
        if (uid == 0) throw IllegalArgumentException("Invalid database, username, or password")

        sessionDataStore.saveSession(
            SessionData(database = database, username = username, password = password, uid = uid),
        )
        OdooUser(uid = uid, database = database, username = username, password = password)
    }

    override suspend fun logout() {
        sessionDataStore.clearSession()
        taskDao.clear()
    }

    override fun getSession(): Flow<OdooUser?> = sessionDataStore.getSession().map { session ->
        session?.let {
            OdooUser(
                uid = it.uid,
                database = it.database,
                username = it.username,
                password = it.password,
            )
        }
    }
}
