package com.example.odootask.domain.usecase.auth

import com.example.odootask.domain.repository.AuthRepository
import javax.inject.Inject

class UpdateAccountUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(name: String): Result<Unit> {
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Name cannot be empty"))
        }
        return repository.updateAccountName(name.trim())
    }
}
