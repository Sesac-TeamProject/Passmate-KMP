package org.sesacteamproject.passmate.ui.mypage

sealed interface MyInfoAction {

    data object Enter : MyInfoAction

    data object Retry : MyInfoAction

    data object LoadMore : MyInfoAction

    data class ClickRoomReport(val roomId: Long) : MyInfoAction

    data class ClickRejoin(val pin: String) : MyInfoAction

    data object ClickCoinHistory : MyInfoAction

    // 내 명성·뱃지 상세(M-09)로 이동
    data object ClickReputation : MyInfoAction

    // 내가 만든 방(M-13)으로 이동
    data object ClickHostedRooms : MyInfoAction

    // 정산(M-T4)으로 이동
    data object ClickEarnings : MyInfoAction

    // 설정(내 정보 관리, M-12)으로 이동
    data object ClickSettings : MyInfoAction
}
