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
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.policy.CoinPolicy
import org.sesacteamproject.passmate.payment.domain.usecase.ConfirmChargeUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.PayEntryFeeUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.RequestChargeUseCase
import org.sesacteamproject.passmate.room.domain.model.RoomInfo
import org.sesacteamproject.passmate.room.domain.policy.JoinInputPolicy
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.JoinRoomUseCase

// 유료 방 결제 입장 (M-01 v2). 흐름: 보유 코인 확인 → 부족 시 포트원 충전 → 참가비 차감 → 입장.
// 최종 차감·자격 판정은 서버(entry-payments 402 등)가 하며, 여기 계산은 UX용이다 (규칙 §8·§13).
class PaymentViewModel(
    private val getRoomInfoUseCase: GetRoomInfoUseCase,
    private val getMyCoinsUseCase: GetMyCoinsUseCase,
    private val requestChargeUseCase: RequestChargeUseCase,
    private val confirmChargeUseCase: ConfirmChargeUseCase,
    private val payEntryFeeUseCase: PayEntryFeeUseCase,
    private val joinRoomUseCase: JoinRoomUseCase,
    private val coinPolicy: CoinPolicy,
    private val joinInputPolicy: JoinInputPolicy
) : MviViewModel<PaymentUiState, PaymentAction, PaymentEvent>(PaymentUiState()) {

    private var pin: String = ""

    private var pendingChargeId: String? = null

    private fun onStart(pin: String) {
        if (this.pin == pin && _uiState.value.room != null) {
            return
        }
        this.pin = pin
        load()
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, hasLoadError = false) }
        viewModelScope.launch {
            val roomResult = getRoomInfoUseCase.invoke(pin)

            roomResult
                .onSuccess { room -> loadCoins(room) }
                .onFailure { _uiState.update { it.copy(isLoading = false, hasLoadError = true) } }
        }
    }

    private suspend fun loadCoins(room: RoomInfo) {
        getMyCoinsUseCase.invoke()
            .onSuccess { coins ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasLoadError = false,
                        room = room,
                        balance = coins.balance,
                        shortfall = coinPolicy.shortfall(coins.balance, room.entryFee ?: 0),
                        selectedMethod = coins.defaultMethod ?: it.selectedMethod
                    )
                }
            }
            .onFailure { _uiState.update { it.copy(isLoading = false, hasLoadError = true) } }
    }

    private fun onChangeNickname(nickname: String) {
        _uiState.update { it.copy(nickname = nickname.take(JoinInputPolicy.NICKNAME_MAX_LENGTH)) }
    }

    private fun onSelectAvatar(avatarId: Int) {
        _uiState.update { it.copy(avatarId = avatarId) }
    }

    private fun onSelectMethod(method: PaymentMethod) {
        _uiState.update { it.copy(selectedMethod = method) }
    }

    private fun onClickPay() {
        val state = _uiState.value

        if (state.isProcessing || state.room == null) {
            return
        }
        if (!joinInputPolicy.isValidNickname(state.nickname)) {
            emitNotice("이 방에서 쓸 닉네임을 입력해 주세요")
        } else if (state.hasEnough) {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            viewModelScope.launch { payEntryAndEnter(state.room) }
        } else {
            // 얼마가 모자란지 먼저 보여 준다 — 바로 결제창을 열면 무슨 금액인지 알 수 없다 (M-11)
            _uiState.update { it.copy(isCoinShortageSheetVisible = true, errorMessage = null) }
        }
    }

    private fun onConfirmCharge() {
        val state = _uiState.value

        if (state.isProcessing) {
            return
        } else {
            _uiState.update { it.copy(isCoinShortageSheetVisible = false, isProcessing = true, errorMessage = null) }
            viewModelScope.launch { startCharge() }
        }
    }

    private suspend fun startCharge() {
        val amount = coinPolicy.suggestedChargeAmount(_uiState.value.shortfall)

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
            is PortOneResult.Success -> viewModelScope.launch { confirmAndEnter(result.paymentId) }
            is PortOneResult.Failure -> _uiState.update { it.copy(isProcessing = false, errorMessage = result.message) }
            is PortOneResult.Cancelled -> _uiState.update { it.copy(isProcessing = false, errorMessage = null) }
        }
    }

    private suspend fun confirmAndEnter(paymentId: String) {
        val chargeId = pendingChargeId
        val room = _uiState.value.room

        if (chargeId == null || room == null) {
            _uiState.update { it.copy(isProcessing = false, errorMessage = "결제 정보를 확인하지 못했어요. 다시 시도해 주세요") }
        } else {
            confirmChargeUseCase.invoke(chargeId, paymentId, roomId = null)
                .onSuccess { confirm ->
                    _uiState.update {
                        it.copy(
                            balance = confirm.balance,
                            shortfall = coinPolicy.shortfall(confirm.balance, room.entryFee ?: 0)
                        )
                    }
                    payEntryAndEnter(room)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isProcessing = false, errorMessage = chargeErrorMessage(error)) }
                }
        }
    }

    private suspend fun payEntryAndEnter(room: RoomInfo) {
        val state = _uiState.value

        payEntryFeeUseCase.invoke(room.roomId, state.nickname, state.avatarId)
            .onSuccess { enterRoom(room) }
            .onFailure { error -> handleEntryFailure(room, error) }
    }

    private suspend fun handleEntryFailure(room: RoomInfo, error: AppError) {
        when (error) {
            is AppError.PaymentRequired -> {
                // 충전 후에도 잔액이 부족한 경쟁 상황 — 다시 충전을 유도한다
                _uiState.update { it.copy(shortfall = coinPolicy.shortfall(it.balance, room.entryFee ?: 0)) }
                startCharge()
            }
            is AppError.LoginRequired, is AppError.Unauthorized -> {
                _uiState.update { it.copy(isProcessing = false) }
                _event.emit(PaymentEvent.SignInRequired)
            }
            else -> _uiState.update { it.copy(isProcessing = false, errorMessage = entryErrorMessage(error)) }
        }
    }

    private suspend fun enterRoom(room: RoomInfo) {
        joinRoomUseCase.invoke(room, _uiState.value.nickname, _uiState.value.avatarId)
            .onSuccess {
                _uiState.update { it.copy(isProcessing = false) }
                _event.emit(PaymentEvent.EnterRoom(room.pin))
            }
            .onFailure { error ->
                _uiState.update { it.copy(isProcessing = false, errorMessage = entryErrorMessage(error)) }
            }
    }

    private fun chargeErrorMessage(error: AppError): String {
        return when (error) {
            is AppError.NetworkError -> "네트워크 연결을 확인해 주세요"
            else -> error.serverMessage ?: "결제에 실패했어요. 다시 시도해 주세요"
        }
    }

    private fun entryErrorMessage(error: AppError): String {
        return when (error) {
            is AppError.Conflict -> "이미 사용 중인 닉네임이에요. 다른 이름을 입력해 주세요"
            is AppError.Gone -> "이미 종료된 방이에요"
            is AppError.NetworkError -> "네트워크 연결을 확인해 주세요"
            else -> error.serverMessage ?: "입장하지 못했어요. 잠시 후 다시 시도해 주세요"
        }
    }

    private fun emitNotice(message: String) {
        viewModelScope.launch { _event.emit(PaymentEvent.ShowNotice(message)) }
    }

    override fun onAction(action: PaymentAction) {
        when (action) {
            is PaymentAction.Start -> onStart(action.pin)
            is PaymentAction.ChangeNickname -> onChangeNickname(action.nickname)
            is PaymentAction.SelectAvatar -> onSelectAvatar(action.avatarId)
            is PaymentAction.SelectMethod -> onSelectMethod(action.method)
            is PaymentAction.ClickPay -> onClickPay()
            is PaymentAction.ConfirmCharge -> onConfirmCharge()
            is PaymentAction.DismissCoinShortage -> _uiState.update { it.copy(isCoinShortageSheetVisible = false) }
            is PaymentAction.ReceivePortOneResult -> onReceivePortOneResult(action.result)
            is PaymentAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            is PaymentAction.Retry -> load()
        }
    }
}
