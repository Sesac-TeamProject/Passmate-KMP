package org.sesacteamproject.passmate.ui.payment

import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction

// 코인 내역 (M-12-9) — 보유 코인 카드 + 전체/충전/사용 필터 + 내역 목록
// balance는 조회 실패 시 null로 두고 카드에서 "-"로 그린다 (목록 실패와 분리)
data class CoinHistoryUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val balance: Int? = null,
    val filter: CoinHistoryFilter = CoinHistoryFilter.ALL,
    val items: List<CoinTransaction> = emptyList(),
    val hasNext: Boolean = false,
    val nextCursor: String? = null,
    val hasError: Boolean = false
) {
    // 필터는 화면 표시 전용 — 서버가 준 페이지는 그대로 두고 보이는 목록만 좁힌다
    val visibleItems: List<CoinTransaction>
        get() = items.filter { filter.matches(it) }

    val isEmpty: Boolean
        get() = !isLoading && !hasError && visibleItems.isEmpty()
}

// M-12-9 필터 칩 — 부호 기준(충전 = 충전·환급·보너스 등 입금, 사용 = 차감)
enum class CoinHistoryFilter(val label: String) {

    ALL("전체"),
    CHARGE("충전"),
    SPEND("사용");

    fun matches(transaction: CoinTransaction): Boolean {
        return when (this) {
            ALL -> true
            CHARGE -> transaction.amount >= 0
            SPEND -> transaction.amount < 0
        }
    }
}
