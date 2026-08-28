package org.sesacteamproject.passmate.ui.mypage

sealed interface MyInfoAction {

    data object Enter : MyInfoAction

    data object Retry : MyInfoAction

    data object LoadMore : MyInfoAction

    data class ClickRoomReport(val roomId: Long) : MyInfoAction

    data class ClickRejoin(val pin: String) : MyInfoAction
}
