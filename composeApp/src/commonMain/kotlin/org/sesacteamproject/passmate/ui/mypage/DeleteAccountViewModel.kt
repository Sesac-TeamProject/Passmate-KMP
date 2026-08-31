package org.sesacteamproject.passmate.ui.mypage

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.user.domain.usecase.DeleteAccountUseCase

// 회원 탈퇴 (M-12-12). 삭제 대상 안내에 실제 보유 코인을 보여주고, 확인 체크 후에만 탈퇴를 실행한다.
// 정산 미지급분·진행 중 방이 있으면 서버가 409로 막는다 — 최종 판정은 서버 (규칙 §8)
class DeleteAccountViewModel(
    private val getMyCoinsUseCase: GetMyCoinsUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase
) : MviViewModel<DeleteAccountUiState, DeleteAccountAction, DeleteAccountEvent>(DeleteAccountUiState()) {

    private fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getMyCoinsUseCase.invoke()
                .onSuccess { coins -> _uiState.update { it.copy(isLoading = false, coins = coins.balance) } }
                .onFailure { _uiState.update { it.copy(isLoading = false) } }
        }
    }

    private fun onClickDelete() {
        if (!_uiState.value.canDelete) {
            return
        }
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            deleteAccountUseCase.invoke()
                .onSuccess {
                    _uiState.update { it.copy(isProcessing = false) }
                    _event.emit(DeleteAccountEvent.Deleted)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isProcessing = false) }
                    _event.emit(DeleteAccountEvent.ShowNotice(deleteFailMessage(error)))
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

    override fun onAction(action: DeleteAccountAction) {
        when (action) {
            is DeleteAccountAction.Enter -> load()
            is DeleteAccountAction.ToggleConfirm -> _uiState.update { it.copy(isConfirmed = !it.isConfirmed) }
            is DeleteAccountAction.ClickDelete -> onClickDelete()
        }
    }
}
