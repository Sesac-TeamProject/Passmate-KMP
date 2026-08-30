package org.sesacteamproject.passmate.ui.mypage

sealed interface JoinedRoomsAction {

    data object Enter : JoinedRoomsAction

    data object Retry : JoinedRoomsAction

    data object LoadMore : JoinedRoomsAction

    data class ClickRoomReport(val roomId: Long) : JoinedRoomsAction

    data class ClickRejoin(val pin: String) : JoinedRoomsAction

    data object ClickCoinHistory : JoinedRoomsAction

    // 내 명성·뱃지 상세(M-09)로 이동
    data object ClickReputation : JoinedRoomsAction

    // 내가 만든 방(M-13)으로 이동
    data object ClickHostedRooms : JoinedRoomsAction

    // 정산(M-T4)으로 이동
    data object ClickEarnings : JoinedRoomsAction

    // 설정(내 정보 관리, M-12)으로 이동
    data object ClickSettings : JoinedRoomsAction
}
