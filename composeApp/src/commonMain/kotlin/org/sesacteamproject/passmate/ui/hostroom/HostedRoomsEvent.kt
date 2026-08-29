package org.sesacteamproject.passmate.ui.hostroom

sealed interface HostedRoomsEvent {

    // 내가 만든 방은 회원 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    data object RequireSignIn : HostedRoomsEvent

    data object OpenCreateSheet : HostedRoomsEvent

    data object OpenReputation : HostedRoomsEvent

    data class ShowNotice(val message: String) : HostedRoomsEvent
}
