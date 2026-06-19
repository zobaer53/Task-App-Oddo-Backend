package com.example.odootask.domain.usecase.auth

import com.example.odootask.domain.model.OdooUser
import com.example.odootask.domain.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        database: String,
        username: String,
        password: String,
    ): Result<OdooUser> {
        if (database.isBlank() || username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("All fields are required"))
        }
        return repository.login(database.trim(), username.trim(), password)
    }
}
