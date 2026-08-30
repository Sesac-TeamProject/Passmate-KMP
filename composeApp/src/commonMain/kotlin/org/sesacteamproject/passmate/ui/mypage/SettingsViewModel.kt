package org.sesacteamproject.passmate.ui.mypage

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.user.domain.usecase.DeleteAccountUseCase

// 설정 (마이 탭 우상단 "설정") — 마이 루트에서 닿지 않는 회원 탈퇴(M-12-12)만 둔다
class SettingsViewModel(
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val isSignedInUseCase: IsSignedInUseCase
) : MviViewModel<SettingsUiState, SettingsAction, SettingsEvent>(SettingsUiState()) {

    private fun onEnter() {
        // 회원 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if (!isSignedInUseCase.invoke()) {
            viewModelScope.launch {
                _event.emit(SettingsEvent.RequireSignIn)
            }
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
                    _event.emit(SettingsEvent.AccountDeleted)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isProcessing = false) }
                    _event.emit(SettingsEvent.ShowNotice(deleteFailMessage(error)))
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

    override fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.Enter -> onEnter()
            is SettingsAction.ConfirmDeleteAccount -> onConfirmDeleteAccount()
        }
    }
}
