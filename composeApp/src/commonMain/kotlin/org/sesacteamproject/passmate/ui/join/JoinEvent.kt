package org.sesacteamproject.passmate.ui.join

sealed interface JoinEvent {

    data object RequestQrScan : JoinEvent

    data class JoinCompleted(val pin: String) : JoinEvent

    data object SignInRequested : JoinEvent

    data class ShowNotice(val message: String) : JoinEvent
}
