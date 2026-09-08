package org.sesacteamproject.passmate.ui.waiting

sealed interface WaitingAction {

    data class Enter(val pin: String) : WaitingAction

    data object ClickLeave : WaitingAction

    // M-07 "지금 다시 연결" — 백오프 대기를 건너뛰고 즉시 재구독한다
    data object Reconnect : WaitingAction
}
