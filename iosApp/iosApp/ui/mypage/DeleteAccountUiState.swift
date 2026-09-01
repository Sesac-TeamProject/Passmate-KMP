import Foundation

// 회원 탈퇴 (M-12-12) — Compose DeleteAccountUiState.kt 미러
struct DeleteAccountUiState {
    var isLoading: Bool = true

    // 안내에 표시할 실제 보유 코인 — 탈퇴 시 환불되지 않는다
    var coins: Int = 0

    var isConfirmed: Bool = false

    // 탈퇴 요청 in-flight — 중복 호출 방지 (규칙 §9)
    var isProcessing: Bool = false

    var canDelete: Bool {
        isConfirmed && !isProcessing
    }
}
