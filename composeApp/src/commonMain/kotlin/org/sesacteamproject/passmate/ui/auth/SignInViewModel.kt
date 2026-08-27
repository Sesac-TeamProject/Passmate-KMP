package org.sesacteamproject.passmate.ui.auth

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.BuildGoogleSignInUrlUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.CompleteSignInUseCase
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel

class SignInViewModel(
    private val buildGoogleSignInUrlUseCase: BuildGoogleSignInUrlUseCase,
    private val completeSignInUseCase: CompleteSignInUseCase
) : MviViewModel<SignInUiState, SignInAction, SignInEvent>(SignInUiState()) {

    private fun onClickGoogleSignIn() {
        val url = buildGoogleSignInUrlUseCase.invoke()

        viewModelScope.launch {
            _event.emit(SignInEvent.OpenSignInPage(url))
        }
    }

    private fun onClickAppleSignIn() {
        viewModelScope.launch {
            _event.emit(SignInEvent.ShowNotice("Apple 로그인은 준비 중이에요"))
        }
    }

    private fun onClickGuestEnter() {
        viewModelScope.launch {
            _event.emit(SignInEvent.GuestEnterRequested)
        }
    }

    private fun onReceiveOAuthCallback(accessToken: String, refreshToken: String) {
        if (_uiState.value.isSigningIn) {
            return
        }
        _uiState.update { it.copy(isSigningIn = true) }
        viewModelScope.launch {
            val result = completeSignInUseCase.invoke(accessToken, refreshToken)

            _uiState.update { it.copy(isSigningIn = false) }
            result
                .onSuccess { _event.emit(SignInEvent.SignInCompleted) }
                .onFailure { _event.emit(SignInEvent.ShowNotice("로그인에 실패했어요. 다시 시도해 주세요")) }
        }
    }

    override fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.ClickGoogleSignIn -> onClickGoogleSignIn()
            is SignInAction.ClickAppleSignIn -> onClickAppleSignIn()
            is SignInAction.ClickGuestEnter -> onClickGuestEnter()
            is SignInAction.ReceiveOAuthCallback -> onReceiveOAuthCallback(action.accessToken, action.refreshToken)
        }
    }
}
