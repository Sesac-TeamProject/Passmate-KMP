package org.sesacteamproject.passmate.ui.mypage

sealed interface MyInfoEvent {

    // 설정은 회원 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    data object RequireSignIn : MyInfoEvent

    data class OpenEditProfile(val nickname: String, val avatarId: Int?) : MyInfoEvent

    data object OpenPaymentMethod : MyInfoEvent

    data object OpenNotifications : MyInfoEvent

    data object OpenCoinHistory : MyInfoEvent

    // 로그아웃·탈퇴 완료 → 홈으로 (세션 정리는 shared가 수행)
    data object SignedOut : MyInfoEvent

    data object AccountDeleted : MyInfoEvent

    data class ShowNotice(val message: String) : MyInfoEvent
}
