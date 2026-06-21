package com.example.odootask.util

import com.example.odootask.domain.model.Project
import com.example.odootask.domain.model.Stage
import com.example.odootask.domain.repository.ProjectRepository

/** Hand-written [ProjectRepository] fake with configurable Results and call counts. */
class FakeProjectRepository : ProjectRepository {

    var projectsResult: Result<List<Project>> = Result.success(listOf(project()))
    var stagesResult: Result<List<Stage>> = Result.success(listOf(stage()))

    var getProjectsCount = 0
    var getStagesCount = 0

    override suspend fun getProjects(): Result<List<Project>> {
        getProjectsCount++
        return projectsResult
    }

    override suspend fun getStages(): Result<List<Stage>> {
        getStagesCount++
        return stagesResult
    }
}
