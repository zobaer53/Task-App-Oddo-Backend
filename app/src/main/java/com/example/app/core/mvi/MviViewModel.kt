package com.example.odootask.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class MviViewModel<S : MviUiState, E : MviUiEvent, F : MviUiEffect>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<F>(capacity = Channel.BUFFERED)
    val effect: Flow<F> = _effect.receiveAsFlow()

    protected fun updateState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    /** Non-blocking fire-and-forget. Use when caller is not in a suspend context. */
    protected fun trySendEffect(effect: F) {
        _effect.trySend(effect)
    }

    /** Suspending send from viewModelScope. Guaranteed delivery even under backpressure. */
    protected fun sendEffect(effect: F) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    abstract fun onEvent(event: E)
}
