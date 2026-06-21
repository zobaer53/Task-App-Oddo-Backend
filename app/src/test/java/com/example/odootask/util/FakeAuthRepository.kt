package com.example.odootask.util

import com.example.odootask.domain.model.OdooUser
import com.example.odootask.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Hand-written [AuthRepository] fake for ViewModel/use-case tests. Configure the next result
 * via the public lambdas/flags; inspect what was called via the recorded fields.
 */
class FakeAuthRepository : AuthRepository {

    private val session = MutableStateFlow<OdooUser?>(null)
    val sessionFlow: StateFlow<OdooUser?> = session.asStateFlow()

    var loginResult: Result<OdooUser> = Result.success(user())
    var accountNameResult: Result<String> = Result.success("Administrator")
    var updateAccountNameResult: Result<Unit> = Result.success(Unit)

    var loginCount = 0
    var logoutCount = 0
    var lastLoginUsername: String? = null
    var lastLoginPassword: String? = null
    var lastUpdatedName: String? = null

    override suspend fun login(username: String, password: String): Result<OdooUser> {
        loginCount++
        lastLoginUsername = username
        lastLoginPassword = password
        return loginResult.onSuccess { session.value = it }
    }

    override suspend fun logout() {
        logoutCount++
        session.value = null
    }

    override fun getSession() = sessionFlow

    override suspend fun getAccountName(): Result<String> = accountNameResult

    override suspend fun updateAccountName(name: String): Result<Unit> {
        lastUpdatedName = name
        return updateAccountNameResult
    }

    /** Seed a logged-in session without going through [login]. */
    fun setSession(odooUser: OdooUser?) {
        session.value = odooUser
    }
}
