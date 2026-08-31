package org.sesacteamproject.passmate.ui.payment

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.sesacteamproject.passmate.component.PortOneRequest
import org.sesacteamproject.passmate.component.PortOneResult
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.onFailure
import org.sesacteamproject.passmate.core.model.onSuccess
import org.sesacteamproject.passmate.mvi.MviViewModel
import org.sesacteamproject.passmate.payment.domain.model.CoinCheckout
import org.sesacteamproject.passmate.payment.domain.policy.CoinPolicy
import org.sesacteamproject.passmate.payment.domain.usecase.ConfirmChargeUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.RequestChargeUseCase

// 코인 충전 (M-12-4·M-12-6). 흐름: 보유 코인 확인 → 금액·수단 선택 → 포트원 충전 → 완료 표시.
// 최종 잔액은 서버 confirm 응답을 그대로 쓴다 — 클라이언트가 더하지 않는다 (규칙 §1 서버 권위)
class CoinChargeViewModel(
    private val getMyCoinsUseCase: GetMyCoinsUseCase,
    private val requestChargeUseCase: RequestChargeUseCase,
    private val confirmChargeUseCase: ConfirmChargeUseCase,
    private val coinPolicy: CoinPolicy
) : MviViewModel<CoinChargeUiState, CoinChargeAction, CoinChargeEvent>(CoinChargeUiState()) {

    private var pendingChargeId: String? = null

    private fun load() {
        _uiState.update { it.copy(isLoading = true, hasLoadError = false, presets = coinPolicy.presets) }
        viewModelScope.launch {
            getMyCoinsUseCase.invoke()
                .onSuccess { coins ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasLoadError = false,
                            balance = coins.balance,
                            selectedMethod = coins.defaultMethod ?: it.selectedMethod
                        )
                    }
                }
                .onFailure { _uiState.update { it.copy(isLoading = false, hasLoadError = true) } }
        }
    }

    private fun onClickCharge() {
        val state = _uiState.value

        if (state.isProcessing || state.isLoading) {
            return
        }
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
        viewModelScope.launch { startCharge(state.selectedAmount) }
    }

    private suspend fun startCharge(amount: Int) {
        requestChargeUseCase.invoke(amount, _uiState.value.selectedMethod, roomId = null)
            .onSuccess { checkout -> showPortOne(checkout) }
            .onFailure { error ->
                _uiState.update { it.copy(isProcessing = false, errorMessage = chargeErrorMessage(error)) }
            }
    }

    private fun showPortOne(checkout: CoinCheckout) {
        pendingChargeId = checkout.chargeId

        _uiState.update {
            it.copy(
                checkout = PortOneRequest(
                    storeId = checkout.storeId,
                    channelKey = checkout.channelKey,
                    paymentId = checkout.paymentId,
                    orderName = checkout.orderName,
                    totalAmount = checkout.amount,
                    currency = checkout.currency,
                    payMethod = checkout.payMethod
                )
            )
        }
    }

    private fun onReceivePortOneResult(result: PortOneResult) {
        _uiState.update { it.copy(checkout = null) }

        when (result) {
            is PortOneResult.Success -> viewModelScope.launch { confirmCharge(result.paymentId) }
            is PortOneResult.Failure -> _uiState.update { it.copy(isProcessing = false, errorMessage = result.message) }
            is PortOneResult.Cancelled -> _uiState.update { it.copy(isProcessing = false, errorMessage = null) }
        }
    }

    private suspend fun confirmCharge(paymentId: String) {
        val chargeId = pendingChargeId
        val amount = _uiState.value.selectedAmount

        if (chargeId == null) {
            _uiState.update { it.copy(isProcessing = false, errorMessage = "결제 정보를 확인하지 못했어요. 다시 시도해 주세요") }
        } else {
            confirmChargeUseCase.invoke(chargeId, paymentId, roomId = null)
                .onSuccess { confirm ->
                    pendingChargeId = null
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            isCompleted = true,
                            balance = confirm.balance,
                            chargedAmount = amount
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isProcessing = false, errorMessage = chargeErrorMessage(error)) }
                }
        }
    }

    private fun chargeErrorMessage(error: AppError): String {
        return when (error) {
            is AppError.NetworkError -> "네트워크 연결을 확인해 주세요"
            else -> error.serverMessage ?: "충전에 실패했어요. 다시 시도해 주세요"
        }
    }

    override fun onAction(action: CoinChargeAction) {
        when (action) {
            is CoinChargeAction.Enter -> load()
            is CoinChargeAction.Retry -> load()
            is CoinChargeAction.SelectAmount -> _uiState.update { it.copy(selectedAmount = action.amount) }
            is CoinChargeAction.SelectMethod -> _uiState.update { it.copy(selectedMethod = action.method) }
            is CoinChargeAction.ClickCharge -> onClickCharge()
            is CoinChargeAction.ReceivePortOneResult -> onReceivePortOneResult(action.result)
            is CoinChargeAction.ClickConfirmDone -> viewModelScope.launch { _event.emit(CoinChargeEvent.Done) }
            is CoinChargeAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }
}
