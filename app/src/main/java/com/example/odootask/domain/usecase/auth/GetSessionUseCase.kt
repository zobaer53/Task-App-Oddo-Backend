package com.example.odootask.domain.usecase.auth

import com.example.odootask.domain.model.OdooUser
import com.example.odootask.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<OdooUser?> = repository.getSession()
}
