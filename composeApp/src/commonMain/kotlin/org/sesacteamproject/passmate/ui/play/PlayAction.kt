package org.sesacteamproject.passmate.ui.play

sealed interface PlayAction {

    data class Enter(val pin: String) : PlayAction

    data class SelectChoice(val index: Int) : PlayAction

    data class ChangeEssayAnswer(val text: String) : PlayAction

    data object ClickSubmit : PlayAction

    data object ClickReplayHint : PlayAction

    data object ConfirmLeave : PlayAction

    data object ClickViewReport : PlayAction

    data object ClickSignup : PlayAction

    // M-07 "지금 다시 연결" — 백오프 대기를 건너뛰고 즉시 재구독한다
    data object Reconnect : PlayAction
}
