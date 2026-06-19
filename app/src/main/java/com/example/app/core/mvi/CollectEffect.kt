package com.example.odootask.core.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow

@Composable
fun <F : MviUiEffect> CollectEffect(
    effect: Flow<F>,
    onEffect: suspend (F) -> Unit,
) {
    val currentOnEffect by rememberUpdatedState(onEffect)
    LaunchedEffect(effect) {
        effect.collect { currentOnEffect(it) }
    }
}
