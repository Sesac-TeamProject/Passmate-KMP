package org.sesacteamproject.passmate.ui.payment

import org.sesacteamproject.passmate.component.PortOneResult

sealed interface CoinChargeAction {

    data object Enter : CoinChargeAction

    data object Retry : CoinChargeAction

    data class SelectAmount(val amount: Int) : CoinChargeAction

    // 주 CTA — 충전 요청 후 바로 포트원 결제창을 띄운다 (결제 수단은 결제창에서 고른다)
    data object ClickCharge : CoinChargeAction

    data class ReceivePortOneResult(val result: PortOneResult) : CoinChargeAction

    // 완료 화면(M-12-6)의 "확인"
    data object ClickConfirmDone : CoinChargeAction

    data object DismissError : CoinChargeAction
}
