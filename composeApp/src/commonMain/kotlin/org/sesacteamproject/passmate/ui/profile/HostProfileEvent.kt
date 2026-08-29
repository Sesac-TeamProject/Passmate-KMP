package org.sesacteamproject.passmate.ui.profile

sealed interface HostProfileEvent {

    // 차단은 회원 전용 — 게스트는 로그인 유도 (규칙 §8)
    data object RequireSignIn : HostProfileEvent

    data class JoinRoom(val pin: String) : HostProfileEvent

    // 차단 완료 — 시트를 닫고 목록을 새로고침한다 (차단 호스트의 방은 공개 목록에서 숨김)
    data object BlockedAndClose : HostProfileEvent

    data class ShowNotice(val message: String) : HostProfileEvent
}
