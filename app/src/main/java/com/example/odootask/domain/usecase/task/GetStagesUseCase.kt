package com.example.odootask.domain.usecase.task

import com.example.odootask.domain.model.Stage
import com.example.odootask.domain.repository.ProjectRepository
import javax.inject.Inject

class GetStagesUseCase @Inject constructor(
    private val repository: ProjectRepository,
) {
    suspend operator fun invoke(): Result<List<Stage>> = repository.getStages()
}
