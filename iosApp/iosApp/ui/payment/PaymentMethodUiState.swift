import Shared

struct PaymentMethodUiState {
    var isLoading: Bool = true

    var selected: PaymentMethod?

    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    var isSubmitting: Bool = false

    var canSubmit: Bool {
        !isSubmitting && selected != nil
    }
}
