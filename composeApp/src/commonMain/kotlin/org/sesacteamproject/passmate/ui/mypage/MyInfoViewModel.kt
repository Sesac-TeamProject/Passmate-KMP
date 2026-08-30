package org.sesacteamproject.passmate.ui.mypage

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.SignOutUseCase
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.user.domain.usecase.DeleteAccountUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetMyProfileUseCase

class MyInfoViewModel(
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<MyInfoUiState, MyInfoAction, MyInfoEvent>(MyInfoUiState()) {

    private var hasEntered = false

    private fun onEnter() {
        if (hasEntered) {
            return
        }
        hasEntered = true
        // 회원 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            viewModelScope.launch {
                _event.emit(MyInfoEvent.RequireSignIn)
            }
        } else {
            load()
        }
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        viewModelScope.launch {
            getMyProfileUseCase.invoke()
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(isLoading = false, loadFailed = false, profile = profile)
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, loadFailed = true) }
                }
        }
    }

    private fun onClickEditProfile() {
        val profile = _uiState.value.profile

        if (profile != null) {
            viewModelScope.launch {
                _event.emit(MyInfoEvent.OpenEditProfile(profile.nickname, profile.avatarId))
            }
        }
    }

    private fun onConfirmSignOut() {
        if (_uiState.value.isProcessing) {
            return
        }
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            // 로컬 세션 정리는 shared가 항상 수행 — 실패 케이스 없음 (M-12-11)
            signOutUseCase.invoke()
            _uiState.update { it.copy(isProcessing = false) }
            _event.emit(MyInfoEvent.SignedOut)
        }
    }

    private fun onConfirmDeleteAccount() {
        if (_uiState.value.isProcessing) {
            return
        }
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            deleteAccountUseCase.invoke()
                .onSuccess {
                    _uiState.update { it.copy(isProcessing = false) }
                    _event.emit(MyInfoEvent.AccountDeleted)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isProcessing = false) }
                    _event.emit(MyInfoEvent.ShowNotice(deleteFailMessage(error)))
                }
        }
    }

    // 서버 code 기반 문구 분기 (규칙 §10) — 409=정산 미지급분·진행 중 방 거부
    private fun deleteFailMessage(error: AppError): String {
        return if (error is AppError.Conflict) {
            error.serverMessage ?: "정산 대기 금액이나 진행 중인 방이 있어 탈퇴할 수 없어요"
        } else if (error is AppError.NetworkError) {
            "네트워크 연결을 확인해 주세요"
        } else {
            "탈퇴를 처리하지 못했어요. 다시 시도해 주세요"
        }
    }

    private fun onProfileUpdated() {
        load()
        emitNotice("내 정보를 저장했어요")
    }

    private fun emitNotice(message: String) {
        viewModelScope.launch {
            _event.emit(MyInfoEvent.ShowNotice(message))
        }
    }

    private fun emit(event: MyInfoEvent) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }

    override fun onAction(action: MyInfoAction) {
        when (action) {
            is MyInfoAction.Enter -> onEnter()
            is MyInfoAction.Retry -> load()
            is MyInfoAction.ClickEditProfile -> onClickEditProfile()
            is MyInfoAction.ClickPaymentMethod -> emit(MyInfoEvent.OpenPaymentMethod)
            is MyInfoAction.ClickNotifications -> emit(MyInfoEvent.OpenNotifications)
            is MyInfoAction.ClickCoinHistory -> emit(MyInfoEvent.OpenCoinHistory)
            is MyInfoAction.ConfirmSignOut -> onConfirmSignOut()
            is MyInfoAction.ConfirmDeleteAccount -> onConfirmDeleteAccount()
            is MyInfoAction.ProfileUpdated -> onProfileUpdated()
            is MyInfoAction.Notice -> emitNotice(action.message)
        }
    }
}
