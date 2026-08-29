package org.sesacteamproject.passmate.ui.payment

data class SettlementAccountUiState(
    val isLoading: Boolean = true,
    val bankName: String = "",
    val accountNumber: String = "",
    val holderName: String = "",
    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    val isSubmitting: Boolean = false
) {

    val canSubmit: Boolean
        get() = !isSubmitting &&
            bankName.isNotBlank() &&
            accountNumber.isNotBlank() &&
            holderName.isNotBlank()
}
