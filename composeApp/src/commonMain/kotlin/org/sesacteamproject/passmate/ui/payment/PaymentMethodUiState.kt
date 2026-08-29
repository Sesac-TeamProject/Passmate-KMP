package org.sesacteamproject.passmate.ui.payment

import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod

data class PaymentMethodUiState(
    val isLoading: Boolean = true,
    val selected: PaymentMethod? = null,
    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    val isSubmitting: Boolean = false
) {

    val canSubmit: Boolean
        get() = !isSubmitting && selected != null
}
