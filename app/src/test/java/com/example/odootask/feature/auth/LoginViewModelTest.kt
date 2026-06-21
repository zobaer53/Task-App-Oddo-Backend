package com.example.odootask.feature.auth

import app.cash.turbine.test
import com.example.odootask.domain.usecase.auth.LoginUseCase
import com.example.odootask.util.FakeAuthRepository
import com.example.odootask.util.MainDispatcherRule
import com.example.odootask.util.user
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * MVI behaviour of [LoginViewModel]: field events reduce into state, the visibility toggle
 * flips, and [LoginUiEvent.LoginClicked] drives the loading flag plus the success/failure
 * branch (navigation effect vs. error message).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepo = FakeAuthRepository()
    private val viewModel = LoginViewModel(LoginUseCase(authRepo))

    @Test
    fun `field-change events update state`() {
        viewModel.onEvent(LoginUiEvent.UsernameChanged("admin"))
        viewModel.onEvent(LoginUiEvent.PasswordChanged("secret"))

        assertThat(viewModel.state.value.username).isEqualTo("admin")
        assertThat(viewModel.state.value.password).isEqualTo("secret")
    }

    @Test
    fun `password visibility toggle flips the flag`() {
        assertThat(viewModel.state.value.isPasswordVisible).isFalse()

        viewModel.onEvent(LoginUiEvent.PasswordVisibilityToggled)
        assertThat(viewModel.state.value.isPasswordVisible).isTrue()

        viewModel.onEvent(LoginUiEvent.PasswordVisibilityToggled)
        assertThat(viewModel.state.value.isPasswordVisible).isFalse()
    }

    @Test
    fun `successful login toggles loading and emits NavigateToTasks`() = runTest {
        authRepo.loginResult = Result.success(user(uid = 7))
        viewModel.onEvent(LoginUiEvent.UsernameChanged("admin"))
        viewModel.onEvent(LoginUiEvent.PasswordChanged("secret"))

        viewModel.effect.test {
            viewModel.onEvent(LoginUiEvent.LoginClicked)
            // updateState runs synchronously before the launched coroutine.
            assertThat(viewModel.state.value.isLoading).isTrue()

            assertThat(awaitItem()).isEqualTo(LoginUiEffect.NavigateToTasks)
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.errorMessage).isNull()
        assertThat(authRepo.loginCount).isEqualTo(1)
    }

    @Test
    fun `failed login clears loading and sets error message`() = runTest {
        authRepo.loginResult = Result.failure(RuntimeException("Invalid credentials"))
        viewModel.onEvent(LoginUiEvent.UsernameChanged("admin"))
        viewModel.onEvent(LoginUiEvent.PasswordChanged("wrong"))

        viewModel.onEvent(LoginUiEvent.LoginClicked)
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.errorMessage).isEqualTo("Invalid credentials")
    }

    @Test
    fun `blank credentials surface the use-case validation message`() = runTest {
        // Username left blank → LoginUseCase rejects before the repository is touched.
        viewModel.onEvent(LoginUiEvent.PasswordChanged("secret"))

        viewModel.onEvent(LoginUiEvent.LoginClicked)
        advanceUntilIdle()

        assertThat(viewModel.state.value.errorMessage).isEqualTo("All fields are required")
        assertThat(authRepo.loginCount).isEqualTo(0)
    }

    @Test
    fun `ErrorDismissed clears the error message`() = runTest {
        authRepo.loginResult = Result.failure(RuntimeException("boom"))
        viewModel.onEvent(LoginUiEvent.UsernameChanged("admin"))
        viewModel.onEvent(LoginUiEvent.PasswordChanged("admin"))
        viewModel.onEvent(LoginUiEvent.LoginClicked)
        advanceUntilIdle()
        assertThat(viewModel.state.value.errorMessage).isNotNull()

        viewModel.onEvent(LoginUiEvent.ErrorDismissed)

        assertThat(viewModel.state.value.errorMessage).isNull()
    }
}
