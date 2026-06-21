package com.example.odootask.ui.navigation

import app.cash.turbine.test
import com.example.odootask.domain.usecase.auth.GetSessionUseCase
import com.example.odootask.util.FakeAuthRepository
import com.example.odootask.util.MainDispatcherRule
import com.example.odootask.util.user
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * [SessionViewModel] maps the session Flow into [SessionState]: a Loading seed resolves to
 * LoggedOut or LoggedIn, and later session changes are reflected. The Loading seed may be
 * conflated away by the backing StateFlow, so it is skipped if not observed.
 */
class SessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepo = FakeAuthRepository()

    @Test
    fun `no session resolves to LoggedOut`() = runTest {
        authRepo.setSession(null)
        val vm = SessionViewModel(GetSessionUseCase(authRepo))

        vm.sessionState.test {
            var s = awaitItem()
            if (s == SessionState.Loading) s = awaitItem()
            assertThat(s).isEqualTo(SessionState.LoggedOut)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `existing session resolves to LoggedIn`() = runTest {
        authRepo.setSession(user())
        val vm = SessionViewModel(GetSessionUseCase(authRepo))

        vm.sessionState.test {
            var s = awaitItem()
            if (s == SessionState.Loading) s = awaitItem()
            assertThat(s).isEqualTo(SessionState.LoggedIn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `logging in after start transitions LoggedOut to LoggedIn`() = runTest {
        authRepo.setSession(null)
        val vm = SessionViewModel(GetSessionUseCase(authRepo))

        vm.sessionState.test {
            var s = awaitItem()
            if (s == SessionState.Loading) s = awaitItem()
            assertThat(s).isEqualTo(SessionState.LoggedOut)

            authRepo.setSession(user())
            assertThat(awaitItem()).isEqualTo(SessionState.LoggedIn)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
