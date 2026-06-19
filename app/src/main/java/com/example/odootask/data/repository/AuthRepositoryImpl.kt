package com.example.odootask.data.repository

import com.example.odootask.data.local.dao.TaskDao
import com.example.odootask.data.local.prefs.SessionData
import com.example.odootask.data.local.prefs.SessionDataStore
import com.example.odootask.BuildConfig
import com.example.odootask.data.remote.OdooApiService
import com.example.odootask.data.remote.dto.CommonParams
import com.example.odootask.data.remote.dto.DbParams
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
 *
 * The database name is resolved automatically (see [resolveDatabase]) instead of
 * being entered by the user, since the target instance is single-database.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: OdooApiService,
    private val sessionDataStore: SessionDataStore,
    private val taskDao: TaskDao,
) : AuthRepository {

    override suspend fun login(
        username: String,
        password: String,
    ): Result<OdooUser> = runCatching {
        val database = resolveDatabase()
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

    /**
     * Resolves the database to authenticate against without prompting the user.
     * Tries the `db` service's `list` method: if the server exposes exactly one
     * database it is used; if several are returned the configured one is preferred.
     * When listing is disabled (common on odoo.com SaaS) we fall back to the
     * [BuildConfig.ODOO_DATABASE] value, which matches the instance subdomain.
     */
    private suspend fun resolveDatabase(): String {
        val configured = BuildConfig.ODOO_DATABASE
        return runCatching {
            val resp = api.callJsonRpc(OdooRequest(params = DbParams()))
            val result = resp.result
            if (result != null && result.isJsonArray) {
                val names = result.asJsonArray
                    .filter { it.isJsonPrimitive }
                    .map { it.asString }
                when {
                    names.isEmpty() -> configured
                    names.size == 1 -> names.first()
                    names.contains(configured) -> configured
                    else -> names.first()
                }
            } else {
                configured
            }
        }.getOrDefault(configured)
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
