import Shared

struct MyInfoUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var profile: UserProfile?

    // 로그아웃·탈퇴 요청 in-flight — 중복 호출 방지 (규칙 §9)
    var isProcessing: Bool = false
}
