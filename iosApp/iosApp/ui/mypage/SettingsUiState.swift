import Foundation

struct SettingsUiState {
    // 탈퇴 요청 in-flight — 중복 호출 방지 (규칙 §9)
    var isProcessing: Bool = false
}
