package com.example.odootask.domain.repository

import com.example.odootask.domain.model.Project
import com.example.odootask.domain.model.Stage

interface ProjectRepository {
    suspend fun getProjects(): Result<List<Project>>
    suspend fun getStages(): Result<List<Stage>>
}
