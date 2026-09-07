package org.sesacteamproject.passmate.ui.auth

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.DevSignInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.IsDevSignInAvailableUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.SignInWithGoogleUseCase
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.user.domain.usecase.CompleteGuestClaimUseCase

class SignInViewModel(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val completeGuestClaimUseCase: CompleteGuestClaimUseCase,
    private val devSignInUseCase: DevSignInUseCase,
    private val isDevSignInAvailableUseCase: IsDevSignInAvailableUseCase
) : MviViewModel<SignInUiState, SignInAction, SignInEvent>(SignInUiState()) {

    // 구글 로그인은 플랫폼 SDK 시트를 여는 것부터 시작한다 — ID 토큰은 액션으로 되돌아온다
    private fun onClickGoogleSignIn() {
        if (!_uiState.value.isSigningIn) {
            _uiState.update { it.copy(isSigningIn = true) }
            viewModelScope.launch {
                _event.emit(SignInEvent.RequestGoogleSignIn)
            }
        }
    }

    // 시트를 닫은 것뿐이면 실패가 아니다 — 진행 표시만 걷는다
    private fun onCancelGoogleSignIn() {
        _uiState.update { it.copy(isSigningIn = false) }
    }

    private fun onFailGoogleSignIn() {
        _uiState.update { it.copy(isSigningIn = false) }
        viewModelScope.launch {
            _event.emit(SignInEvent.ShowNotice("구글 로그인을 마치지 못했어요. 다시 시도해 주세요"))
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

    // 개발용 로그인 — 서버가 바로 토큰 쌍을 주므로 브라우저 왕복이 없다
    private fun onClickDevSignIn() {
        if (!_uiState.value.isSigningIn) {
            _uiState.update { it.copy(isSigningIn = true) }
            viewModelScope.launch {
                executeDevSignIn()
            }
        }
    }

    private suspend fun executeDevSignIn() {
        val result = devSignInUseCase.invoke()

        _uiState.update { it.copy(isSigningIn = false) }
        result
            .onSuccess { finishSignIn() }
            .onFailure { _event.emit(SignInEvent.ShowNotice("개발 로그인에 실패했어요. 로컬 백엔드가 떠 있는지 확인해 주세요")) }
    }

    private fun onReceiveGoogleIdToken(idToken: String) {
        _uiState.update { it.copy(isSigningIn = true) }
        viewModelScope.launch {
            executeGoogleSignIn(idToken)
        }
    }

    private suspend fun executeGoogleSignIn(idToken: String) {
        val result = signInWithGoogleUseCase.invoke(idToken)

        _uiState.update { it.copy(isSigningIn = false) }
        result
            .onSuccess { finishSignIn() }
            .onFailure { _event.emit(SignInEvent.ShowNotice("로그인에 실패했어요. 다시 시도해 주세요")) }
    }

    private suspend fun finishSignIn() {
        // 가입 유도로 진입했다면 대기 중인 게스트 기록을 연동한다 (FR-036)
        claimPendingGuestRecord()
        _event.emit(SignInEvent.SignInCompleted)
    }

    private suspend fun claimPendingGuestRecord() {
        val claimResult = completeGuestClaimUseCase.invoke() ?: return

        claimResult.onFailure { error ->
            if (error is AppError.Gone) {
                _event.emit(SignInEvent.ShowNotice("기록 보관 기간(7일)이 지나 저장하지 못했어요"))
            } else {
                _event.emit(SignInEvent.ShowNotice("기록을 계정에 저장하지 못했어요"))
            }
        }
    }

    override fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.ClickGoogleSignIn -> onClickGoogleSignIn()
            is SignInAction.ClickAppleSignIn -> onClickAppleSignIn()
            is SignInAction.ClickGuestEnter -> onClickGuestEnter()
            is SignInAction.ClickDevSignIn -> onClickDevSignIn()
            is SignInAction.ReceiveGoogleIdToken -> onReceiveGoogleIdToken(action.idToken)
            is SignInAction.CancelGoogleSignIn -> onCancelGoogleSignIn()
            is SignInAction.FailGoogleSignIn -> onFailGoogleSignIn()
        }
    }

    init {
        _uiState.update { it.copy(isDevSignInAvailable = isDevSignInAvailableUseCase.invoke()) }
    }
}
