package org.sesacteamproject.passmate.ui.hostroom

sealed interface HostedRoomsAction {

    data object Enter : HostedRoomsAction

    data object Retry : HostedRoomsAction

    data object LoadMore : HostedRoomsAction

    // + FAB → 새 방 만들기 시트 (시트 표시는 화면이 소유)
    data object ClickCreate : HostedRoomsAction

    data object ClickReputation : HostedRoomsAction

    data class ClickOngoingRoom(val roomId: Long, val pin: String) : HostedRoomsAction

    data class ClickEndedRoom(val roomId: Long) : HostedRoomsAction

    // 시트에서 방 생성 완료 — 목록 새로고침 + PIN 안내
    data class RoomCreated(val pin: String) : HostedRoomsAction

    data class Notice(val message: String) : HostedRoomsAction
}
