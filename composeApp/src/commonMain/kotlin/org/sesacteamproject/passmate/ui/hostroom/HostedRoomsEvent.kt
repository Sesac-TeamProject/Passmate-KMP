package org.sesacteamproject.passmate.ui.hostroom

sealed interface HostedRoomsEvent {

    // 내가 만든 방은 회원 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    data object RequireSignIn : HostedRoomsEvent

    data object OpenCreateSheet : HostedRoomsEvent

    data object OpenReputation : HostedRoomsEvent

    // 종료된 방 상세 → 방 리포트 (M-14)
    data class OpenRoomReport(val roomId: Long) : HostedRoomsEvent

    data class ShowNotice(val message: String) : HostedRoomsEvent
}
