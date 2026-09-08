package org.sesacteamproject.passmate.ui.waiting

sealed interface WaitingEvent {

    data class SessionStarted(val pin: String) : WaitingEvent

    // 세션이 끝났다 — 대기실을 백스택에서 걷어내고 결과로 보낸다 (규칙 §2-1-2)
    data class SessionFinished(val roomId: Long) : WaitingEvent

    data class RoomClosed(val message: String) : WaitingEvent

    data object Left : WaitingEvent

    data class ShowNotice(val message: String) : WaitingEvent
}
