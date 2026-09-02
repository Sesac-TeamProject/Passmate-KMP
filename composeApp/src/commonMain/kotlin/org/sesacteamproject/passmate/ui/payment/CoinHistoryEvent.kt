package org.sesacteamproject.passmate.ui.payment

sealed interface CoinHistoryEvent {

    data class ShowNotice(val message: String) : CoinHistoryEvent

    // 빈 상태 CTA — 코인 충전 화면으로 이동
    data object OpenCoinCharge : CoinHistoryEvent
}
