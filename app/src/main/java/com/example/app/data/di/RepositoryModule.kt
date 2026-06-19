package com.example.odootask.data.di

import com.example.odootask.data.repository.AuthRepositoryImpl
import com.example.odootask.data.repository.ProjectRepositoryImpl
import com.example.odootask.data.repository.TaskRepositoryImpl
import com.example.odootask.domain.repository.AuthRepository
import com.example.odootask.domain.repository.ProjectRepository
import com.example.odootask.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository
}
