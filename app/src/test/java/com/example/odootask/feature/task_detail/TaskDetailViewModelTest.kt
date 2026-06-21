package com.example.odootask.feature.task_detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.odootask.domain.usecase.task.GetStagesUseCase
import com.example.odootask.domain.usecase.task.GetTaskUseCase
import com.example.odootask.domain.usecase.task.UpdateTaskStageUseCase
import com.example.odootask.util.FakeProjectRepository
import com.example.odootask.util.FakeTaskRepository
import com.example.odootask.util.MainDispatcherRule
import com.example.odootask.util.stage
import com.example.odootask.util.task
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * MVI behaviour of [TaskDetailViewModel]: the taskId is read from [SavedStateHandle], Appeared
 * loads the task plus stages and pre-selects the current stage, [TaskDetailUiState.canUpdate]
 * gates the action, and UpdateClicked navigates back on success / snackbars on failure.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val taskRepo = FakeTaskRepository()
    private val projectRepo = FakeProjectRepository()

    private val stages = listOf(stage(1, "To Do"), stage(2, "In Progress"), stage(3, "Done"))

    private fun viewModel(taskId: Int = 1) = TaskDetailViewModel(
        getTaskUseCase = GetTaskUseCase(taskRepo),
        getStagesUseCase = GetStagesUseCase(projectRepo),
        updateTaskStageUseCase = UpdateTaskStageUseCase(taskRepo),
        savedStateHandle = SavedStateHandle(mapOf("taskId" to taskId)),
    )

    @Test
    fun `Appeared loads task and stages and pre-selects the current stage`() = runTest {
        taskRepo.getTaskHandler = { task(id = 1, stageId = 2, stageName = "In Progress") }
        projectRepo.stagesResult = Result.success(stages)
        val vm = viewModel(taskId = 1)

        vm.onEvent(TaskDetailUiEvent.Appeared)
        advanceUntilIdle()

        assertThat(vm.state.value.task?.id).isEqualTo(1)
        assertThat(vm.state.value.stages).isEqualTo(stages)
        assertThat(vm.state.value.selectedStage).isEqualTo(stage(2, "In Progress"))
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `Appeared loads only once`() = runTest {
        taskRepo.getTaskHandler = { task(id = 1, stageId = 2) }
        projectRepo.stagesResult = Result.success(stages)
        val vm = viewModel(taskId = 1)

        vm.onEvent(TaskDetailUiEvent.Appeared)
        advanceUntilIdle()
        vm.onEvent(TaskDetailUiEvent.Appeared)
        advanceUntilIdle()

        assertThat(projectRepo.getStagesCount).isEqualTo(1)
    }

    @Test
    fun `missing task sets a not-found error`() = runTest {
        taskRepo.getTaskHandler = { null }
        val vm = viewModel(taskId = 99)

        vm.onEvent(TaskDetailUiEvent.Appeared)
        advanceUntilIdle()

        assertThat(vm.state.value.task).isNull()
        assertThat(vm.state.value.errorMessage).isEqualTo("Task not found")
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `canUpdate is false until a different stage is picked`() = runTest {
        taskRepo.getTaskHandler = { task(id = 1, stageId = 2, stageName = "In Progress") }
        projectRepo.stagesResult = Result.success(stages)
        val vm = viewModel(taskId = 1)
        // No task loaded yet → cannot update.
        assertThat(vm.state.value.canUpdate).isFalse()

        vm.onEvent(TaskDetailUiEvent.Appeared)
        advanceUntilIdle()
        // Pre-selected stage equals the current one → still false.
        assertThat(vm.state.value.canUpdate).isFalse()

        vm.onEvent(TaskDetailUiEvent.StageSelected(stage(3, "Done")))
        assertThat(vm.state.value.selectedStage).isEqualTo(stage(3, "Done"))
        assertThat(vm.state.value.canUpdate).isTrue()
    }

    @Test
    fun `UpdateClicked success updates the stage and navigates back`() = runTest {
        taskRepo.getTaskHandler = { task(id = 1, stageId = 2) }
        projectRepo.stagesResult = Result.success(stages)
        val vm = viewModel(taskId = 1)
        vm.onEvent(TaskDetailUiEvent.Appeared)
        advanceUntilIdle()
        vm.onEvent(TaskDetailUiEvent.StageSelected(stage(3, "Done")))

        vm.effect.test {
            vm.onEvent(TaskDetailUiEvent.UpdateClicked)
            assertThat(awaitItem()).isEqualTo(TaskDetailUiEffect.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(taskRepo.lastUpdateStage).isEqualTo(1 to 3)
        assertThat(vm.state.value.isUpdating).isFalse()
    }

    @Test
    fun `UpdateClicked failure emits a snackbar`() = runTest {
        taskRepo.getTaskHandler = { task(id = 1, stageId = 2) }
        projectRepo.stagesResult = Result.success(stages)
        taskRepo.updateStageResult = Result.failure(RuntimeException("write failed"))
        val vm = viewModel(taskId = 1)
        vm.onEvent(TaskDetailUiEvent.Appeared)
        advanceUntilIdle()
        vm.onEvent(TaskDetailUiEvent.StageSelected(stage(3, "Done")))

        vm.effect.test {
            vm.onEvent(TaskDetailUiEvent.UpdateClicked)
            assertThat(awaitItem()).isEqualTo(TaskDetailUiEffect.ShowSnackbar("write failed"))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(vm.state.value.isUpdating).isFalse()
    }

    @Test
    fun `BackClicked emits NavigateBack`() = runTest {
        val vm = viewModel()

        vm.effect.test {
            vm.onEvent(TaskDetailUiEvent.BackClicked)
            assertThat(awaitItem()).isEqualTo(TaskDetailUiEffect.NavigateBack)
        }
    }
}
