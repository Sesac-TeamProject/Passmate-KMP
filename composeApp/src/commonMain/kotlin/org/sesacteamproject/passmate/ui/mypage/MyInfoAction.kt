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
}
