package com.example.odootask.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.odootask.domain.usecase.auth.GetSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Resolves whether a session exists so the nav host can pick its start destination. */
sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedIn : SessionState
    data object LoggedOut : SessionState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    getSessionUseCase: GetSessionUseCase,
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = getSessionUseCase()
        .map { user -> if (user != null) SessionState.LoggedIn else SessionState.LoggedOut }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionState.Loading,
        )
}
