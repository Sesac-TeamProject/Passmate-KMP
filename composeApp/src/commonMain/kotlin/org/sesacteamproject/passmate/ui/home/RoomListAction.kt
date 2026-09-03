package org.sesacteamproject.passmate.ui.home

import org.sesacteamproject.passmate.payment.domain.model.RoomTypeFilter

sealed interface RoomListAction {

    data class ChangeQuery(val query: String) : RoomListAction

    data object SubmitSearch : RoomListAction

    data class SelectType(val type: RoomTypeFilter) : RoomListAction

    // 목록 응답에 pin이 없어 roomId로 받는다 — pin은 ViewModel이 조회한다
    data class ClickRoom(val roomId: Long) : RoomListAction

    // 선생님 이름 탭 → 프로필 시트 (M-10)
    data class ClickHost(val hostId: Long) : RoomListAction

    data object LoadMore : RoomListAction

    data object Retry : RoomListAction

    data object ClickPinEntry : RoomListAction

    // 프로필 시트(M-10) 등 화면 위 계층에서 올라온 안내 문구 — 스낵바로 노출
    data class Notice(val message: String) : RoomListAction
}
