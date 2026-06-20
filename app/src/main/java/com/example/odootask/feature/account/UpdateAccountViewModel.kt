package com.example.odootask.feature.account

import androidx.lifecycle.viewModelScope
import com.example.odootask.core.mvi.MviViewModel
import com.example.odootask.domain.usecase.auth.GetAccountNameUseCase
import com.example.odootask.domain.usecase.auth.UpdateAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateAccountViewModel @Inject constructor(
    private val getAccountNameUseCase: GetAccountNameUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
) : MviViewModel<UpdateAccountUiState, UpdateAccountUiEvent, UpdateAccountUiEffect>(UpdateAccountUiState()) {

    private var hasLoaded = false

    override fun onEvent(event: UpdateAccountUiEvent) {
        when (event) {
            UpdateAccountUiEvent.Appeared -> onAppeared()
            is UpdateAccountUiEvent.NameChanged -> updateState { copy(name = event.v) }
            UpdateAccountUiEvent.SaveClicked -> saveAccount()
            UpdateAccountUiEvent.BackClicked -> sendEffect(UpdateAccountUiEffect.NavigateBack)
            UpdateAccountUiEvent.ErrorDismissed -> updateState { copy(errorMessage = null) }
        }
    }

    /** Pre-fills the field with the current account name once per ViewModel. */
    private fun onAppeared() {
        if (hasLoaded) return
        hasLoaded = true
        updateState { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            getAccountNameUseCase().fold(
                onSuccess = { name -> updateState { copy(isLoading = false, name = name) } },
                onFailure = { e ->
                    updateState { copy(isLoading = false) }
                    sendEffect(UpdateAccountUiEffect.ShowSnackbar(e.message ?: "Failed to load account"))
                },
            )
        }
    }

    private fun saveAccount() {
        val state = state.value
        if (!state.canSave) return

        updateState { copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            updateAccountUseCase(state.name).fold(
                onSuccess = {
                    updateState { copy(isSaving = false) }
                    sendEffect(UpdateAccountUiEffect.ShowSnackbar("Account updated"))
                    sendEffect(UpdateAccountUiEffect.NavigateBack)
                },
                onFailure = { e ->
                    updateState { copy(isSaving = false) }
                    sendEffect(UpdateAccountUiEffect.ShowSnackbar(e.message ?: "Failed to update account"))
                },
            )
        }
    }
}
