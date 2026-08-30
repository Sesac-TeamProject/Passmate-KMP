package org.sesacteamproject.passmate.ui.mypage

sealed interface SettingsEvent {
    // 설정은 회원 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    data object RequireSignIn : SettingsEvent

    // 탈퇴 완료 → 홈으로 (세션 정리는 shared가 수행)
    data object AccountDeleted : SettingsEvent

    data class ShowNotice(val message: String) : SettingsEvent
}
