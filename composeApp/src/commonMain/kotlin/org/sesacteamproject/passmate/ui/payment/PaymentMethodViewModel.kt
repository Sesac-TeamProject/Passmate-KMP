package org.sesacteamproject.passmate.ui.payment

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.SetPaymentMethodUseCase

class PaymentMethodViewModel(
    private val getMyCoinsUseCase: GetMyCoinsUseCase,
    private val setPaymentMethodUseCase: SetPaymentMethodUseCase
) : MviViewModel<PaymentMethodUiState, PaymentMethodAction, PaymentMethodEvent>(PaymentMethodUiState()) {

    private var hasEntered = false

    private fun onEnter() {
        if (hasEntered) {
            return
        }
        hasEntered = true
        loadDefault()
    }

    // 현재 기본 결제 수단은 내 코인 조회 응답의 defaultMethod 재사용 (GET /users/me/coins)
    private fun loadDefault() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getMyCoinsUseCase.invoke()
                .onSuccess { balance ->
                    _uiState.update {
                        it.copy(isLoading = false, selected = it.selected ?: balance.defaultMethod)
                    }
                }
                .onFailure {
                    // 기본값 로드 실패는 빈 선택으로 시작 — 저장 자체는 막지 않는다
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun onSubmit() {
        val state = _uiState.value
        val selected = state.selected

        if (!state.canSubmit || selected == null) {
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            setPaymentMethodUseCase.invoke(selected)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _event.emit(PaymentMethodEvent.Saved)
                }
                .onFailure {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _event.emit(PaymentMethodEvent.ShowNotice("결제 수단을 저장하지 못했어요. 다시 시도해 주세요"))
                }
        }
    }

    override fun onAction(action: PaymentMethodAction) {
        when (action) {
            is PaymentMethodAction.Enter -> onEnter()
            is PaymentMethodAction.Select -> _uiState.update { it.copy(selected = action.method) }
            is PaymentMethodAction.Submit -> onSubmit()
        }
    }
}
