package org.sesacteamproject.passmate.ui.mypage

// 회원 탈퇴 (M-12-12) — 삭제 대상 안내 + 확인 체크 + 탈퇴
data class DeleteAccountUiState(
    val isLoading: Boolean = true,
    // 안내에 표시할 실제 보유 코인 — 탈퇴 시 환불되지 않는다
    val coins: Int = 0,
    val isConfirmed: Boolean = false,
    // 탈퇴 요청 in-flight — 중복 호출 방지 (규칙 §9)
    val isProcessing: Boolean = false
) {
    val canDelete: Boolean
        get() = isConfirmed && !isProcessing
}
