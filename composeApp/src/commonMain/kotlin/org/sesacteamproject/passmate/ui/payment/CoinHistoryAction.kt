package org.sesacteamproject.passmate.ui.payment

sealed interface CoinHistoryAction {

    data object Enter : CoinHistoryAction

    data object Retry : CoinHistoryAction

    data object LoadMore : CoinHistoryAction
}
