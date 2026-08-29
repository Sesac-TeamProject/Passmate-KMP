package org.sesacteamproject.passmate.ui.hostroom

sealed interface CreateRoomAction {

    data object Enter : CreateRoomAction

    data object RetrySets : CreateRoomAction

    data class ChangeTitle(val title: String) : CreateRoomAction

    data class SelectSet(val setId: Long) : CreateRoomAction

    data class SelectPaid(val isPaid: Boolean) : CreateRoomAction

    data class ChangeEntryFee(val text: String) : CreateRoomAction

    data object Submit : CreateRoomAction
}
