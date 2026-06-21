package com.example.odootask.domain.usecase.auth

import com.example.odootask.util.FakeAuthRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** [UpdateAccountUseCase] rejects a blank name and otherwise delegates a trimmed name. */
class UpdateAccountUseCaseTest {

    private val repo = FakeAuthRepository()
    private val useCase = UpdateAccountUseCase(repo)

    @Test
    fun `blank name fails without hitting repository`() = runTest {
        val result = useCase("   ")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(repo.lastUpdatedName).isNull()
    }

    @Test
    fun `valid name delegates trimmed and forwards success`() = runTest {
        val result = useCase("  New Name  ")

        assertThat(result.isSuccess).isTrue()
        assertThat(repo.lastUpdatedName).isEqualTo("New Name")
    }

    @Test
    fun `repository failure is forwarded`() = runTest {
        repo.updateAccountNameResult = Result.failure(RuntimeException("nope"))

        val result = useCase("New Name")

        assertThat(result.isFailure).isTrue()
    }
}
