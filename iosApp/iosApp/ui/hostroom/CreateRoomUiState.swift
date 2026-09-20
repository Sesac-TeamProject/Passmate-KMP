import Shared

struct CreateRoomUiState {
    var isLoadingSets: Bool = true

    var setsLoadFailed: Bool = false

    var sets: [QuestionSetSummary] = []

    var title: String = ""

    var selectedSetId: Int64?

    var isPaid: Bool = false

    var entryFeeText: String = ""

    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    var isSubmitting: Bool = false

    // 베타 잠금 (M-13aβ) — 유료 탭을 "유료 · 준비 중"으로 끄고 배너를 띄운다. 값은 VM이 BetaConfig에서 채운다
    var isBetaPaymentLocked: Bool = false

    var selectedSet: QuestionSetSummary? {
        sets.first { $0.setId == selectedSetId }
    }

    var canSubmit: Bool {
        !isSubmitting &&
            !title.trimmingCharacters(in: .whitespaces).isEmpty &&
            selectedSetId != nil &&
            (!isPaid || (Int(entryFeeText) ?? 0) > 0)
    }
}
