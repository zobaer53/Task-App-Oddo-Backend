package com.example.odootask.domain.usecase.task

import com.example.odootask.util.FakeTaskRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** [DeleteTaskUseCase] guards against an invalid task id and otherwise delegates. */
class DeleteTaskUseCaseTest {

    private val repo = FakeTaskRepository()
    private val useCase = DeleteTaskUseCase(repo)

    @Test
    fun `non-positive task id fails without hitting repository`() = runTest {
        val result = useCase(taskId = 0)

        assertThat(result.isFailure).isTrue()
        assertThat(repo.lastDeletedTaskId).isNull()
    }

    @Test
    fun `valid id delegates to repository`() = runTest {
        val result = useCase(taskId = 8)

        assertThat(result.isSuccess).isTrue()
        assertThat(repo.lastDeletedTaskId).isEqualTo(8)
    }

    @Test
    fun `repository failure is forwarded`() = runTest {
        repo.deleteResult = Result.failure(RuntimeException("unlink failed"))

        val result = useCase(taskId = 8)

        assertThat(result.isFailure).isTrue()
    }
}
