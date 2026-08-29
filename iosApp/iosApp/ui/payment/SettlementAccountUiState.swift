struct SettlementAccountUiState {
    var isLoading: Bool = true

    var bankName: String = ""

    var accountNumber: String = ""

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
