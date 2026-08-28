package org.sesacteamproject.passmate.ui.home

import org.sesacteamproject.passmate.payment.domain.model.RoomTypeFilter

sealed interface RoomListAction {

    data class ChangeQuery(val query: String) : RoomListAction

    data object SubmitSearch : RoomListAction

    data class SelectType(val type: RoomTypeFilter) : RoomListAction

    data class ClickRoom(val pin: String) : RoomListAction

    data object LoadMore : RoomListAction

    data object Retry : RoomListAction

    data object ClickPinEntry : RoomListAction
}
