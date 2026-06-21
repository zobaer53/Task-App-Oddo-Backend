package com.example.odootask.feature.account

import app.cash.turbine.test
import com.example.odootask.domain.usecase.auth.GetAccountNameUseCase
import com.example.odootask.domain.usecase.auth.UpdateAccountUseCase
import com.example.odootask.util.FakeAuthRepository
import com.example.odootask.util.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * MVI behaviour of [UpdateAccountViewModel]: Appeared pre-fills the current name once,
 * [UpdateAccountUiState.canSave] gates the action, and SaveClicked navigates back with a
 * confirmation on success / a snackbar on failure.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateAccountViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepo = FakeAuthRepository()

    private fun viewModel() = UpdateAccountViewModel(
        getAccountNameUseCase = GetAccountNameUseCase(authRepo),
        updateAccountUseCase = UpdateAccountUseCase(authRepo),
    )

    @Test
    fun `Appeared pre-fills the current name once`() = runTest {
        authRepo.accountNameResult = Result.success("Administrator")
        val vm = viewModel()

        vm.onEvent(UpdateAccountUiEvent.Appeared)
        advanceUntilIdle()
        assertThat(vm.state.value.name).isEqualTo("Administrator")
        assertThat(vm.state.value.isLoading).isFalse()

        // A second appearance must not reload (would overwrite an in-progress edit).
        vm.onEvent(UpdateAccountUiEvent.NameChanged("Edited"))
        vm.onEvent(UpdateAccountUiEvent.Appeared)
        advanceUntilIdle()
        assertThat(vm.state.value.name).isEqualTo("Edited")
    }

    @Test
    fun `Appeared failure emits a snackbar`() = runTest {
        authRepo.accountNameResult = Result.failure(RuntimeException("load failed"))
        val vm = viewModel()

        vm.effect.test {
            vm.onEvent(UpdateAccountUiEvent.Appeared)
            assertThat(awaitItem()).isEqualTo(UpdateAccountUiEffect.ShowSnackbar("load failed"))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(vm.state.value.isLoading).isFalse()
    }

    @Test
    fun `canSave requires a non-blank name and no work in flight`() {
        val vm = viewModel()
        assertThat(vm.state.value.canSave).isFalse()

        vm.onEvent(UpdateAccountUiEvent.NameChanged("New Name"))
        assertThat(vm.state.value.canSave).isTrue()
    }

    @Test
    fun `SaveClicked success forwards the name then navigates back`() = runTest {
        val vm = viewModel()
        vm.onEvent(UpdateAccountUiEvent.NameChanged("New Name"))

        vm.effect.test {
            vm.onEvent(UpdateAccountUiEvent.SaveClicked)
            assertThat(awaitItem()).isEqualTo(UpdateAccountUiEffect.ShowSnackbar("Account updated"))
            assertThat(awaitItem()).isEqualTo(UpdateAccountUiEffect.NavigateBack)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(authRepo.lastUpdatedName).isEqualTo("New Name")
        assertThat(vm.state.value.isSaving).isFalse()
    }

    @Test
    fun `SaveClicked failure emits a snackbar`() = runTest {
        authRepo.updateAccountNameResult = Result.failure(RuntimeException("update failed"))
        val vm = viewModel()
        vm.onEvent(UpdateAccountUiEvent.NameChanged("New Name"))

        vm.effect.test {
            vm.onEvent(UpdateAccountUiEvent.SaveClicked)
            assertThat(awaitItem()).isEqualTo(UpdateAccountUiEffect.ShowSnackbar("update failed"))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(vm.state.value.isSaving).isFalse()
    }

    @Test
    fun `SaveClicked is ignored while the name is blank`() = runTest {
        val vm = viewModel()

        vm.onEvent(UpdateAccountUiEvent.SaveClicked)
        advanceUntilIdle()

        assertThat(authRepo.lastUpdatedName).isNull()
    }

    @Test
    fun `BackClicked emits NavigateBack`() = runTest {
        val vm = viewModel()

        vm.effect.test {
            vm.onEvent(UpdateAccountUiEvent.BackClicked)
            assertThat(awaitItem()).isEqualTo(UpdateAccountUiEffect.NavigateBack)
        }
    }
}
