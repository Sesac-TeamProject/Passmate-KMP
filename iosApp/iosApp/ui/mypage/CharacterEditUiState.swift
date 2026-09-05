// Compose CharacterEditUiState.kt 미러 (M-12-7)
struct CharacterEditUiState {
    var avatarId: Int?

    var isLoading: Bool = true

    var hasLoadError: Bool = false

    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    var isSubmitting: Bool = false

    var canSubmit: Bool {
        !isLoading && !isSubmitting && avatarId != nil
    }
}
