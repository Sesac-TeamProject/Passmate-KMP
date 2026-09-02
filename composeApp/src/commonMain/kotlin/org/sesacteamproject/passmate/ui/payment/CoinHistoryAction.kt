package org.sesacteamproject.passmate.ui.payment

sealed interface CoinHistoryAction {

    data object Enter : CoinHistoryAction

    data object Retry : CoinHistoryAction

    data object LoadMore : CoinHistoryAction

    // M-12-9 필터 칩 (전체·충전·사용)
    data class SelectFilter(val filter: CoinHistoryFilter) : CoinHistoryAction

    // 빈 상태 CTA "코인 충전하기"
    data object ClickCharge : CoinHistoryAction
}
