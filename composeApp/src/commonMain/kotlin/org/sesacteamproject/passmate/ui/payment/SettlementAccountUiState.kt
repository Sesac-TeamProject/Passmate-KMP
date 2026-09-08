package org.sesacteamproject.passmate.ui.payment

data class SettlementAccountUiState(
    val isLoading: Boolean = true,
    // 드롭다운에서 고른 은행 — 코드는 저장 요청 필수값, 이름은 표시용 (M-12-3)
    val bankCode: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    // 이미 등록된 계좌의 마스킹 번호 — 안내 표시용, 저장에는 쓰지 않는다
    val maskedAccountNumber: String = "",
    val holderName: String = "",
    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    val isSubmitting: Boolean = false
) {

    val canSubmit: Boolean
        get() = !isSubmitting &&
            bankCode.isNotBlank() &&
            accountNumber.isNotBlank() &&
            holderName.isNotBlank()
}
