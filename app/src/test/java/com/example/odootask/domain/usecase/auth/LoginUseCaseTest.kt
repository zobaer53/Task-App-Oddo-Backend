package com.example.odootask.domain.usecase.auth

import com.example.odootask.util.FakeAuthRepository
import com.example.odootask.util.user
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Validation for [LoginUseCase]: blank credentials are rejected before the repository is
 * touched, otherwise the call is delegated (with a trimmed username) and the [Result] forwarded.
 */
class LoginUseCaseTest {

    private val repo = FakeAuthRepository()
    private val useCase = LoginUseCase(repo)

    @Test
    fun `blank username fails without hitting repository`() = runTest {
        val result = useCase(username = "   ", password = "admin")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(repo.loginCount).isEqualTo(0)
    }

    @Test
    fun `blank password fails without hitting repository`() = runTest {
        val result = useCase(username = "admin", password = "")

        assertThat(result.isFailure).isTrue()
        assertThat(repo.loginCount).isEqualTo(0)
    }

    @Test
    fun `valid credentials delegate with trimmed username and forward success`() = runTest {
        val expected = user(uid = 7)
        repo.loginResult = Result.success(expected)

        val result = useCase(username = "  admin  ", password = "secret")

        assertThat(result.getOrNull()).isEqualTo(expected)
        assertThat(repo.loginCount).isEqualTo(1)
        assertThat(repo.lastLoginUsername).isEqualTo("admin")
        assertThat(repo.lastLoginPassword).isEqualTo("secret")
    }

    @Test
    fun `repository failure is forwarded unchanged`() = runTest {
        val boom = RuntimeException("network down")
        repo.loginResult = Result.failure(boom)

        val result = useCase(username = "admin", password = "admin")

        assertThat(result.exceptionOrNull()).isSameInstanceAs(boom)
    }
}
