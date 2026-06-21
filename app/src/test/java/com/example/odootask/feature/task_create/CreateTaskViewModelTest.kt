package com.example.odootask.feature.task_create

import app.cash.turbine.test
import com.example.odootask.domain.usecase.auth.GetSessionUseCase
import com.example.odootask.domain.usecase.task.CreateTaskUseCase
import com.example.odootask.domain.usecase.task.GetProjectsUseCase
import com.example.odootask.util.FakeAuthRepository
import com.example.odootask.util.FakeProjectRepository
import com.example.odootask.util.FakeTaskRepository
import com.example.odootask.util.MainDispatcherRule
import com.example.odootask.util.project
import com.example.odootask.util.user
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * MVI behaviour of [CreateTaskViewModel]: Appeared silently resolves the current uid and target
 * project, [CreateTaskUiState.canCreate] gates the action, and CreateClicked forwards the right
 * args then emits snackbar + navigate-back (or a snackbar when no project is available).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateTaskViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val taskRepo = FakeTaskRepository()
    private val projectRepo = FakeProjectRepository()
    private val authRepo = FakeAuthRepository()

    private fun viewModel() = CreateTaskViewModel(
        createTaskUseCase = CreateTaskUseCase(taskRepo),
        getProjectsUseCase = GetProjectsUseCase(projectRepo),
        getSessionUseCase = GetSessionUseCase(authRepo),
    )

    @Test
    fun `field-change events update state`() {
        val vm = viewModel()

        vm.onEvent(CreateTaskUiEvent.NameChanged("Buy milk"))
        vm.onEvent(CreateTaskUiEvent.DescriptionChanged("2 litres"))
        vm.onEvent(CreateTaskUiEvent.DeadlineSelected("2026-07-01"))

        assertThat(vm.state.value.name).isEqualTo("Buy milk")
        assertThat(vm.state.value.description).isEqualTo("2 litres")
        assertThat(vm.state.value.deadline).isEqualTo("2026-07-01")
    }

    @Test
    fun `canCreate requires a non-blank name`() {
        val vm = viewModel()
        assertThat(vm.state.value.canCreate).isFalse()

        vm.onEvent(CreateTaskUiEvent.NameChanged("Something"))
        assertThat(vm.state.value.canCreate).isTrue()
    }

    @Test
    fun `CreateClicked forwards resolved uid and project then navigates back`() = runTest {
        authRepo.setSession(user(uid = 5))
        projectRepo.projectsResult = Result.success(listOf(project(id = 10)))
        taskRepo.createTaskResult = Result.success(77)
        val vm = viewModel()

        vm.onEvent(CreateTaskUiEvent.Appeared)
        advanceUntilIdle()
        vm.onEvent(CreateTaskUiEvent.NameChanged("My task"))
        vm.onEvent(CreateTaskUiEvent.DeadlineSelected("2026-07-01"))

        vm.effect.test {
            vm.onEvent(CreateTaskUiEvent.CreateClicked)
            assertThat(awaitItem()).isEqualTo(CreateTaskUiEffect.ShowSnackbar("Task created"))
            assertThat(awaitItem()).isEqualTo(CreateTaskUiEffect.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }

        val create = taskRepo.lastCreate
        assertThat(create).isNotNull()
        assertThat(create!!.name).isEqualTo("My task")
        assertThat(create.projectId).isEqualTo(10)
        assertThat(create.dateDeadline).isEqualTo("2026-07-01")
        assertThat(create.userIds).containsExactly(5)
        assertThat(vm.state.value.isSaving).isFalse()
    }

    @Test
    fun `CreateClicked with no project available emits a snackbar`() = runTest {
        authRepo.setSession(user(uid = 5))
        projectRepo.projectsResult = Result.success(emptyList())
        val vm = viewModel()

        vm.onEvent(CreateTaskUiEvent.Appeared)
        advanceUntilIdle()
        vm.onEvent(CreateTaskUiEvent.NameChanged("My task"))

        vm.effect.test {
            vm.onEvent(CreateTaskUiEvent.CreateClicked)
            assertThat(awaitItem())
                .isEqualTo(CreateTaskUiEffect.ShowSnackbar("No project available to create the task in"))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(taskRepo.lastCreate).isNull()
    }

    @Test
    fun `CreateClicked is ignored while the name is blank`() = runTest {
        authRepo.setSession(user(uid = 5))
        projectRepo.projectsResult = Result.success(listOf(project(id = 10)))
        val vm = viewModel()
        vm.onEvent(CreateTaskUiEvent.Appeared)
        advanceUntilIdle()

        vm.onEvent(CreateTaskUiEvent.CreateClicked)
        advanceUntilIdle()

        assertThat(taskRepo.lastCreate).isNull()
    }

    @Test
    fun `create failure emits an error snackbar`() = runTest {
        authRepo.setSession(user(uid = 5))
        projectRepo.projectsResult = Result.success(listOf(project(id = 10)))
        taskRepo.createTaskResult = Result.failure(RuntimeException("save failed"))
        val vm = viewModel()
        vm.onEvent(CreateTaskUiEvent.Appeared)
        advanceUntilIdle()
        vm.onEvent(CreateTaskUiEvent.NameChanged("My task"))

        vm.effect.test {
            vm.onEvent(CreateTaskUiEvent.CreateClicked)
            assertThat(awaitItem()).isEqualTo(CreateTaskUiEffect.ShowSnackbar("save failed"))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(vm.state.value.isSaving).isFalse()
    }

    @Test
    fun `BackClicked emits NavigateBack`() = runTest {
        val vm = viewModel()

        vm.effect.test {
            vm.onEvent(CreateTaskUiEvent.BackClicked)
            assertThat(awaitItem()).isEqualTo(CreateTaskUiEffect.NavigateBack)
        }
    }
}
