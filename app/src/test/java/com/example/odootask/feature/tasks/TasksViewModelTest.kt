package com.example.odootask.feature.tasks

import app.cash.turbine.test
import com.example.odootask.domain.usecase.auth.GetAccountNameUseCase
import com.example.odootask.domain.usecase.auth.GetSessionUseCase
import com.example.odootask.domain.usecase.auth.LogoutUseCase
import com.example.odootask.domain.usecase.task.DeleteTaskUseCase
import com.example.odootask.domain.usecase.task.GetTasksUseCase
import com.example.odootask.domain.usecase.task.ObserveTasksUseCase
import com.example.odootask.util.FakeAuthRepository
import com.example.odootask.util.FakeTaskRepository
import com.example.odootask.util.MainDispatcherRule
import com.example.odootask.util.task
import com.example.odootask.util.user
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * MVI behaviour of [TasksViewModel]: the observed Room flow drives the list, the greeting
 * refreshes each appearance while the list loads only once (hasLoaded guard), refresh/delete/
 * logout/navigation events produce the right state changes and effects.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepo = FakeAuthRepository()
    private val taskRepo = FakeTaskRepository()

    private fun viewModel() = TasksViewModel(
        observeTasksUseCase = ObserveTasksUseCase(taskRepo),
        getTasksUseCase = GetTasksUseCase(taskRepo),
        deleteTaskUseCase = DeleteTaskUseCase(taskRepo),
        logoutUseCase = LogoutUseCase(authRepo),
        getSessionUseCase = GetSessionUseCase(authRepo),
        getAccountNameUseCase = GetAccountNameUseCase(authRepo),
    )

    @Test
    fun `observed task flow drives state tasks`() = runTest {
        val vm = viewModel()
        advanceUntilIdle() // start the init collection

        val tasks = listOf(task(id = 1), task(id = 2))
        taskRepo.emitTasks(tasks)
        advanceUntilIdle()

        assertThat(vm.state.value.tasks).isEqualTo(tasks)
    }

    @Test
    fun `Appeared loads tasks once and refreshes the greeting each time`() = runTest {
        authRepo.setSession(user(username = "admin@odoo.com"))
        authRepo.accountNameResult = Result.success("Administrator")
        val vm = viewModel()

        vm.onEvent(TasksUiEvent.Appeared)
        advanceUntilIdle()
        assertThat(taskRepo.getTasksCount).isEqualTo(1)
        assertThat(vm.state.value.email).isEqualTo("admin@odoo.com")
        assertThat(vm.state.value.name).isEqualTo("Administrator")

        // A second appearance refreshes the greeting but must not reload the list.
        authRepo.accountNameResult = Result.success("New Name")
        vm.onEvent(TasksUiEvent.Appeared)
        advanceUntilIdle()
        assertThat(taskRepo.getTasksCount).isEqualTo(1)
        assertThat(vm.state.value.name).isEqualTo("New Name")
    }

    @Test
    fun `RefreshPulled sets isRefreshing then clears it`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.onEvent(TasksUiEvent.RefreshPulled)
        // updateState runs synchronously before the launched load.
        assertThat(vm.state.value.isRefreshing).isTrue()

        advanceUntilIdle()
        assertThat(vm.state.value.isRefreshing).isFalse()
        assertThat(taskRepo.getTasksCount).isEqualTo(1)
    }

    @Test
    fun `load failure emits a snackbar and clears the loading flags`() = runTest {
        taskRepo.getTasksResult = Result.failure(RuntimeException("network down"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.effect.test {
            vm.onEvent(TasksUiEvent.Appeared)
            assertThat(awaitItem()).isEqualTo(TasksUiEffect.ShowSnackbar("network down"))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(vm.state.value.isLoading).isFalse()
        assertThat(vm.state.value.isRefreshing).isFalse()
    }

    @Test
    fun `TaskClicked emits NavigateToTaskDetail with the id`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.effect.test {
            vm.onEvent(TasksUiEvent.TaskClicked(taskId = 9))
            assertThat(awaitItem()).isEqualTo(TasksUiEffect.NavigateToTaskDetail(9))
        }
    }

    @Test
    fun `CreateTaskClicked and UpdateAccountClicked emit their navigation effects`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.effect.test {
            vm.onEvent(TasksUiEvent.CreateTaskClicked)
            assertThat(awaitItem()).isEqualTo(TasksUiEffect.NavigateToCreateTask)
            vm.onEvent(TasksUiEvent.UpdateAccountClicked)
            assertThat(awaitItem()).isEqualTo(TasksUiEffect.NavigateToUpdateAccount)
        }
    }

    @Test
    fun `LogoutClicked logs out and emits NavigateToLogin`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.effect.test {
            vm.onEvent(TasksUiEvent.LogoutClicked)
            assertThat(awaitItem()).isEqualTo(TasksUiEffect.NavigateToLogin)
        }
        assertThat(authRepo.logoutCount).isEqualTo(1)
    }

    @Test
    fun `TaskDeleted success emits a confirmation snackbar`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.effect.test {
            vm.onEvent(TasksUiEvent.TaskDeleted(taskId = 3))
            assertThat(awaitItem()).isEqualTo(TasksUiEffect.ShowSnackbar("Task deleted"))
        }
        assertThat(taskRepo.lastDeletedTaskId).isEqualTo(3)
    }

    @Test
    fun `TaskDeleted failure emits an error snackbar`() = runTest {
        taskRepo.deleteResult = Result.failure(RuntimeException("cannot delete"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.effect.test {
            vm.onEvent(TasksUiEvent.TaskDeleted(taskId = 3))
            assertThat(awaitItem()).isEqualTo(TasksUiEffect.ShowSnackbar("cannot delete"))
        }
    }

    @Test
    fun `ErrorDismissed clears the error message`() {
        val vm = viewModel()
        vm.onEvent(TasksUiEvent.ErrorDismissed)
        assertThat(vm.state.value.errorMessage).isNull()
    }
}
