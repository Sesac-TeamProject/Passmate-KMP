package org.sesacteamproject.passmate.ui.payment

import org.sesacteamproject.passmate.component.PortOneRequest
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod

// 코인 충전 (M-12-4·M-12-6) — 금액·결제 수단 선택과 충전 완료 표시.
// 완료 화면은 별도 라우트가 아니라 isCompleted 전환으로 같은 라우트 안에서 그린다
data class CoinChargeUiState(
    val isLoading: Boolean = true,
    val hasLoadError: Boolean = false,
    val balance: Int = 0,
    val presets: List<Int> = emptyList(),
    val selectedAmount: Int = DEFAULT_AMOUNT,
    val selectedMethod: PaymentMethod = PaymentMethod.KAKAO_PAY,
    val isProcessing: Boolean = false,
    val checkout: PortOneRequest? = null,
    val isCompleted: Boolean = false,
    // 완료 화면 표기용 — 방금 충전한 금액(원 = C)
    val chargedAmount: Int = 0,
    val errorMessage: String? = null
) {
    val isPortOneVisible: Boolean
        get() = checkout != null
}

// 시안 M-12-4의 기본 선택 금액
private const val DEFAULT_AMOUNT: Int = 10_000
