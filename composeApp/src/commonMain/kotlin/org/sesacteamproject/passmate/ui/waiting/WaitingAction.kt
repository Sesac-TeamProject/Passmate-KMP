package org.sesacteamproject.passmate.ui.waiting

sealed interface WaitingAction {

    data class Enter(val pin: String) : WaitingAction

    data object ClickLeave : WaitingAction
}
