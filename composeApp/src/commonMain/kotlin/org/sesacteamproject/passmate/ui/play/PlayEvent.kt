package org.sesacteamproject.passmate.ui.play

sealed interface PlayEvent {

    data class RoomClosed(val message: String) : PlayEvent

    data object Left : PlayEvent

    data class ShowNotice(val message: String) : PlayEvent
}
