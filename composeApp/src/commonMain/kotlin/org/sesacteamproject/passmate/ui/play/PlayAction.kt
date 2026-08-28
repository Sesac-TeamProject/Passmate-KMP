package org.sesacteamproject.passmate.ui.play

sealed interface PlayAction {

    data class Enter(val pin: String) : PlayAction

    data class SelectChoice(val index: Int) : PlayAction

    data class ChangeEssayAnswer(val text: String) : PlayAction

    data object ClickSubmit : PlayAction

    data object ConfirmLeave : PlayAction

    data object ClickViewReport : PlayAction
}
