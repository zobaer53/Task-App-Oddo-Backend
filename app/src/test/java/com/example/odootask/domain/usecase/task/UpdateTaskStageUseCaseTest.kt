package com.example.odootask.domain.usecase.task

import com.example.odootask.util.FakeTaskRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** [UpdateTaskStageUseCase] guards against an invalid task id or an unselected stage. */
class UpdateTaskStageUseCaseTest {

    private val repo = FakeTaskRepository()
    private val useCase = UpdateTaskStageUseCase(repo)

    @Test
    fun `non-positive task id fails without hitting repository`() = runTest {
        val result = useCase(taskId = 0, stageId = 2)

        assertThat(result.isFailure).isTrue()
        assertThat(repo.lastUpdateStage).isNull()
    }

    @Test
    fun `non-positive stage id fails without hitting repository`() = runTest {
        val result = useCase(taskId = 1, stageId = 0)

        assertThat(result.isFailure).isTrue()
        assertThat(repo.lastUpdateStage).isNull()
    }

    @Test
    fun `valid ids delegate to repository`() = runTest {
        val result = useCase(taskId = 5, stageId = 3)

        assertThat(result.isSuccess).isTrue()
        assertThat(repo.lastUpdateStage).isEqualTo(5 to 3)
    }

    @Test
    fun `repository failure is forwarded`() = runTest {
        repo.updateStageResult = Result.failure(RuntimeException("write failed"))

        val result = useCase(taskId = 5, stageId = 3)

        assertThat(result.isFailure).isTrue()
    }
}
