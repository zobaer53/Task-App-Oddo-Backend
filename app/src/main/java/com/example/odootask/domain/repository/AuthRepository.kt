package com.example.odootask.domain.repository

import com.example.odootask.domain.model.OdooUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<OdooUser>
    suspend fun logout()
    fun getSession(): Flow<OdooUser?>
}
