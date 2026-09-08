package org.sesacteamproject.passmate.ui.waiting

sealed interface WaitingAction {

    data class Enter(val pin: String) : WaitingAction

    // 참가자 조회 실패·연결 끊김 후 다시 불러오기
    data object RetryParticipants : WaitingAction

    data object ClickLeave : WaitingAction

    // M-07 "지금 다시 연결" — 백오프 대기를 건너뛰고 즉시 재구독한다
    data object Reconnect : WaitingAction
}
