package com.example.odootask.domain.usecase

import app.cash.turbine.test
import com.example.odootask.domain.usecase.auth.GetAccountNameUseCase
import com.example.odootask.domain.usecase.auth.GetSessionUseCase
import com.example.odootask.domain.usecase.auth.LogoutUseCase
import com.example.odootask.domain.usecase.task.GetProjectsUseCase
import com.example.odootask.domain.usecase.task.GetStagesUseCase
import com.example.odootask.domain.usecase.task.GetTaskUseCase
import com.example.odootask.domain.usecase.task.GetTasksUseCase
import com.example.odootask.domain.usecase.task.ObserveTasksUseCase
import com.example.odootask.util.FakeAuthRepository
import com.example.odootask.util.FakeProjectRepository
import com.example.odootask.util.FakeTaskRepository
import com.example.odootask.util.project
import com.example.odootask.util.stage
import com.example.odootask.util.task
import com.example.odootask.util.user
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The validation-free use cases are pure delegators. One focused test each proves they forward
 * the call to the right repository method and pass the [Result]/value/Flow straight through.
 */
class DelegatingUseCasesTest {

    private val authRepo = FakeAuthRepository()
    private val taskRepo = FakeTaskRepository()
    private val projectRepo = FakeProjectRepository()

    @Test
    fun `GetTasksUseCase forwards repository result`() = runTest {
        taskRepo.getTasksResult = Result.success(listOf(task(id = 1), task(id = 2)))

        val result = GetTasksUseCase(taskRepo)()

        assertThat(result.getOrNull()).hasSize(2)
        assertThat(taskRepo.getTasksCount).isEqualTo(1)
    }

    @Test
    fun `GetTaskUseCase forwards single task`() = runTest {
        taskRepo.emitTasks(listOf(task(id = 5)))

        val result = GetTaskUseCase(taskRepo)(5)

        assertThat(result).isEqualTo(task(id = 5))
    }

    @Test
    fun `ObserveTasksUseCase exposes the repository flow`() = runTest {
        ObserveTasksUseCase(taskRepo)().test {
            assertThat(awaitItem()).isEmpty()
            taskRepo.emitTasks(listOf(task(id = 1)))
            assertThat(awaitItem()).hasSize(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GetProjectsUseCase forwards repository result`() = runTest {
        projectRepo.projectsResult = Result.success(listOf(project(id = 1), project(id = 2)))

        val result = GetProjectsUseCase(projectRepo)()

        assertThat(result.getOrNull()).hasSize(2)
        assertThat(projectRepo.getProjectsCount).isEqualTo(1)
    }

    @Test
    fun `GetStagesUseCase forwards repository result`() = runTest {
        projectRepo.stagesResult = Result.success(listOf(stage(id = 1), stage(id = 2)))

        val result = GetStagesUseCase(projectRepo)()

        assertThat(result.getOrNull()).hasSize(2)
        assertThat(projectRepo.getStagesCount).isEqualTo(1)
    }

    @Test
    fun `GetAccountNameUseCase forwards repository result`() = runTest {
        authRepo.accountNameResult = Result.success("Administrator")

        val result = GetAccountNameUseCase(authRepo)()

        assertThat(result.getOrNull()).isEqualTo("Administrator")
    }

    @Test
    fun `LogoutUseCase delegates to repository`() = runTest {
        LogoutUseCase(authRepo)()

        assertThat(authRepo.logoutCount).isEqualTo(1)
    }

    @Test
    fun `GetSessionUseCase exposes the session flow`() = runTest {
        GetSessionUseCase(authRepo)().test {
            assertThat(awaitItem()).isNull()
            authRepo.setSession(user(uid = 1))
            assertThat(awaitItem()).isEqualTo(user(uid = 1))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
