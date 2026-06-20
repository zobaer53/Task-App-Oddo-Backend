package com.example.odootask.domain.usecase.auth

import com.example.odootask.domain.repository.AuthRepository
import javax.inject.Inject

class GetAccountNameUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): Result<String> = repository.getAccountName()
}
