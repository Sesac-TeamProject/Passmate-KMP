// Compose EditProfileUiState.kt 미러 (M-12-1)
struct EditProfileUiState {
    var nickname: String = ""

    // 이메일은 로그인 ID라 표시만 하고 바꾸지 않는다 (시안 M-12-1)
    var email: String?

    // 캐릭터는 M-12-7에서 바꾼다 — 여기서는 현재 값을 보여 주기만 한다
    var avatarId: Int?

    var isLoading: Bool = true

    var hasLoadError: Bool = false

    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    var isSubmitting: Bool = false

    var canSubmit: Bool {
        !isLoading && !isSubmitting && !nickname.trimmingCharacters(in: .whitespaces).isEmpty
    }
}
