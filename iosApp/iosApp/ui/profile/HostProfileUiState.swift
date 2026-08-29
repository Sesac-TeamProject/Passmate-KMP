import Shared

struct HostProfileUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    var profile: HostProfile?

    // 제출 in-flight — 중복 호출 방지 (규칙 §9)
    var isSubmitting: Bool = false

    var isReported: Bool = false
}
