package org.sesacteamproject.passmate.ui.payment

sealed interface CoinChargeEvent {

    // 완료 화면에서 "확인" — 마이로 돌아간다
    data object Done : CoinChargeEvent

    data class ShowNotice(val message: String) : CoinChargeEvent
}
