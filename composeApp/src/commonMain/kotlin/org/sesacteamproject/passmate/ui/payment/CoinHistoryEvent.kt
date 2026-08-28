package org.sesacteamproject.passmate.ui.payment

sealed interface CoinHistoryEvent {

    data class ShowNotice(val message: String) : CoinHistoryEvent
}
