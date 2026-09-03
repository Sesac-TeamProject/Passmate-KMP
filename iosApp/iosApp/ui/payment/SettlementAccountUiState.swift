struct SettlementAccountUiState {
    var isLoading: Bool = true

    var bankName: String = ""

    var accountNumber: String = ""

    // 이미 등록된 계좌의 마스킹 번호 — 안내 표시용, 저장에는 쓰지 않는다
    var maskedAccountNumber: String = ""

    var holderName: String = ""

    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    var isSubmitting: Bool = false

    var canSubmit: Bool {
        !isSubmitting &&
            !bankName.trimmingCharacters(in: .whitespaces).isEmpty &&
            !accountNumber.trimmingCharacters(in: .whitespaces).isEmpty &&
            !holderName.trimmingCharacters(in: .whitespaces).isEmpty
    }
}
