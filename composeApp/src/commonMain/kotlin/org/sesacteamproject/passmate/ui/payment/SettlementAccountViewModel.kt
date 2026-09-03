package org.sesacteamproject.passmate.ui.payment

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccount
import org.sesacteamproject.passmate.payment.domain.usecase.GetSettlementAccountUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.SaveSettlementAccountUseCase

class SettlementAccountViewModel(
    private val getSettlementAccountUseCase: GetSettlementAccountUseCase,
    private val saveSettlementAccountUseCase: SaveSettlementAccountUseCase
) : MviViewModel<SettlementAccountUiState, SettlementAccountAction, SettlementAccountEvent>(SettlementAccountUiState()) {

    private var hasEntered = false

    private fun onEnter() {
        if (hasEntered) {
            return
        }
        hasEntered = true
        load()
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getSettlementAccountUseCase.invoke()
                .onSuccess { account ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            bankName = account.bankName,
                            // 조회는 마스킹된 번호만 준다 — 그대로 저장하면 실제 번호가 덮인다.
                            // 편집 필드는 비우고 마스킹 값은 안내로만 보여준다.
                            accountNumber = "",
                            maskedAccountNumber = account.maskedAccountNumber,
                            holderName = account.holderName
                        )
                    }
                }
                .onFailure { error ->
                    // 미등록(404)은 빈 폼으로 시작한다 (M-12-3)
                    _uiState.update { it.copy(isLoading = false) }
                    if (error !is AppError.NotFound) {
                        _event.emit(SettlementAccountEvent.ShowNotice("계좌 정보를 불러오지 못했어요"))
                    }
                }
        }
    }

    private fun onSubmit() {
        val state = _uiState.value

        if (!state.canSubmit) {
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val account = SettlementAccount(
                bankName = state.bankName,
                maskedAccountNumber = state.accountNumber,
                holderName = state.holderName
            )

            saveSettlementAccountUseCase.invoke(account)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _event.emit(SettlementAccountEvent.Saved)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _event.emit(SettlementAccountEvent.ShowNotice(saveFailMessage(error)))
                }
        }
    }

    // 서버 code 기반 문구 분기 (규칙 §10)
    private fun saveFailMessage(error: AppError): String {
        return if (error is AppError.ValidationFailed) {
            error.serverMessage ?: "계좌 정보를 확인해 주세요"
        } else if (error is AppError.NetworkError) {
            "네트워크 연결을 확인해 주세요"
        } else {
            "계좌를 저장하지 못했어요. 다시 시도해 주세요"
        }
    }

    override fun onAction(action: SettlementAccountAction) {
        when (action) {
            is SettlementAccountAction.Enter -> onEnter()
            is SettlementAccountAction.ChangeBankName -> _uiState.update { it.copy(bankName = action.text) }
            is SettlementAccountAction.ChangeAccountNumber -> _uiState.update {
                it.copy(accountNumber = action.text.filter { ch -> ch.isDigit() || ch == '-' }.take(20))
            }
            is SettlementAccountAction.ChangeHolderName -> _uiState.update { it.copy(holderName = action.text) }
            is SettlementAccountAction.Submit -> onSubmit()
        }
    }
}
