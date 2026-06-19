package com.example.odootask.domain.usecase.task

import com.example.odootask.domain.model.Project
import com.example.odootask.domain.repository.ProjectRepository
import javax.inject.Inject

class GetProjectsUseCase @Inject constructor(
    private val repository: ProjectRepository,
) {
    suspend operator fun invoke(): Result<List<Project>> = repository.getProjects()
}
