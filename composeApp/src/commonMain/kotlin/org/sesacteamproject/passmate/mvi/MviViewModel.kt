package org.sesacteamproject.passmate.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class MviViewModel<S : Any, A : Any, E : Any>(initialState: S) : ViewModel() {
    protected val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    protected val _event = MutableSharedFlow<E>(replay = 0, extraBufferCapacity = 1)
    val event: SharedFlow<E> = _event.asSharedFlow()

    abstract fun onAction(action: A)
}
