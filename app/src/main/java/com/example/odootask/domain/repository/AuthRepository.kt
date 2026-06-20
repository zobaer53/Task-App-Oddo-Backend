package com.example.odootask.domain.repository

import com.example.odootask.domain.model.OdooUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<OdooUser>
    suspend fun logout()
    fun getSession(): Flow<OdooUser?>

    /** Reads the display name of the currently logged-in `res.users` record. */
    suspend fun getAccountName(): Result<String>

    /** Writes a new display name onto the currently logged-in `res.users` record. */
    suspend fun updateAccountName(name: String): Result<Unit>
}
