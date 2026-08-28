package org.sesacteamproject.passmate.ui.payment

import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction

// 코인 사용·충전 내역 (M-12) — 충전·차감·환급 목록
data class CoinHistoryUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val items: List<CoinTransaction> = emptyList(),
    val hasNext: Boolean = false,
    val nextCursor: String? = null,
    val hasError: Boolean = false
) {
    val isEmpty: Boolean
        get() = !isLoading && !hasError && items.isEmpty()
}
