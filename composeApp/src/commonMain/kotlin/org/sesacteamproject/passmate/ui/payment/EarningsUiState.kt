package org.sesacteamproject.passmate.ui.payment

import org.sesacteamproject.passmate.payment.domain.model.Earnings
import org.sesacteamproject.passmate.payment.domain.model.SettlementItem

data class EarningsUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    // 요약·계좌 정보 — 목록은 페이징 append 때문에 items로 분리 관리한다
    val earnings: Earnings? = null,
    val items: List<SettlementItem> = emptyList(),
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false
)
