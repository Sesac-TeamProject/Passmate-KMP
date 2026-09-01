package org.sesacteamproject.passmate.ui.payment

import org.sesacteamproject.passmate.component.PortOneResult
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod

sealed interface CoinChargeAction {

    data object Enter : CoinChargeAction

    data object Retry : CoinChargeAction

    data class SelectAmount(val amount: Int) : CoinChargeAction

    data class SelectMethod(val method: PaymentMethod) : CoinChargeAction

    // 주 CTA — 충전 요청 후 포트원 결제창을 띄운다
    data object ClickCharge : CoinChargeAction

    data class ReceivePortOneResult(val result: PortOneResult) : CoinChargeAction

    // 완료 화면(M-12-6)의 "확인"
    data object ClickConfirmDone : CoinChargeAction

    data object DismissError : CoinChargeAction
}
