package org.sesacteamproject.passmate.ui.mypage

sealed interface JoinedRoomsAction {
    data object Enter : JoinedRoomsAction

    data object Retry : JoinedRoomsAction

    data object LoadMore : JoinedRoomsAction

    data class ClickRoomReport(val roomId: Long) : JoinedRoomsAction

    data class ClickRejoin(val pin: String) : JoinedRoomsAction

    // 목록 불러오기 실패 화면의 "계속 안 되면 문의하기" (v6 E-List 실패 공통)
    data object ClickContactSupport : JoinedRoomsAction
}
