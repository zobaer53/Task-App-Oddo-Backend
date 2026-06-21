package com.example.odootask.domain.usecase.task

import com.example.odootask.util.FakeTaskRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * [CreateTaskUseCase] validation: a blank name or a non-positive project id is rejected before
 * the repository is called; valid input is delegated with a trimmed name/description.
 */
class CreateTaskUseCaseTest {

    private val repo = FakeTaskRepository()
    private val useCase = CreateTaskUseCase(repo)

    @Test
    fun `blank name fails without hitting repository`() = runTest {
        val result = useCase(name = "  ", projectId = 1, description = null, dateDeadline = null, userIds = emptyList())

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(repo.lastCreate).isNull()
    }

    @Test
    fun `non-positive project id fails without hitting repository`() = runTest {
        val result = useCase(name = "Task", projectId = 0, description = null, dateDeadline = null, userIds = emptyList())

        assertThat(result.isFailure).isTrue()
        assertThat(repo.lastCreate).isNull()
    }

    @Test
    fun `valid input delegates with trimmed name and description and forwards id`() = runTest {
        repo.createTaskResult = Result.success(99)

        val result = useCase(
            name = "  Fix bug  ",
            projectId = 3,
            description = "  details  ",
            dateDeadline = "2026-07-01",
            userIds = listOf(1, 2),
        )

        assertThat(result.getOrNull()).isEqualTo(99)
        val args = repo.lastCreate!!
        assertThat(args.name).isEqualTo("Fix bug")
        assertThat(args.projectId).isEqualTo(3)
        assertThat(args.description).isEqualTo("details")
        assertThat(args.dateDeadline).isEqualTo("2026-07-01")
        assertThat(args.userIds).containsExactly(1, 2).inOrder()
    }

    @Test
    fun `null description is passed through untouched`() = runTest {
        useCase(name = "Task", projectId = 1, description = null, dateDeadline = null, userIds = emptyList())

        assertThat(repo.lastCreate!!.description).isNull()
    }
}
