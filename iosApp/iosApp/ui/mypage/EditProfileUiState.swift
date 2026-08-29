struct EditProfileUiState {
    var nickname: String = ""

    var avatarId: Int?

    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    var isSubmitting: Bool = false

    var canSubmit: Bool {
        !isSubmitting && !nickname.trimmingCharacters(in: .whitespaces).isEmpty
    }
}
