package org.sesacteamproject.passmate.ui.mypage

sealed interface JoinedRoomsAction {
    data object Enter : JoinedRoomsAction

    data object Retry : JoinedRoomsAction

    data object LoadMore : JoinedRoomsAction

    data class ClickRoomReport(val roomId: Long) : JoinedRoomsAction

    data class ClickRejoin(val pin: String) : JoinedRoomsAction

    // 빈 상태 CTA — 홈 탭(=PIN 입장 폼)으로 보낸다 (규칙 §2-1-1)
    data object ClickEnterPin : JoinedRoomsAction
}
