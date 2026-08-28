package org.sesacteamproject.passmate.ui.waiting

sealed interface WaitingEvent {

    data class SessionStarted(val pin: String) : WaitingEvent

    data class RoomClosed(val message: String) : WaitingEvent

    data object Left : WaitingEvent

    data class ShowNotice(val message: String) : WaitingEvent
}
