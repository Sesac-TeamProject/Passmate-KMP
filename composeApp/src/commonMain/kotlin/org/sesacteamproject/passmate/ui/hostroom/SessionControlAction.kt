package org.sesacteamproject.passmate.ui.hostroom

sealed interface SessionControlAction {

    data class Enter(val roomId: Long, val pin: String) : SessionControlAction

    data object Retry : SessionControlAction

    data object ClickStart : SessionControlAction

    data object ClickNext : SessionControlAction

    data object ClickEndQuestion : SessionControlAction

    // 확인 다이얼로그를 거친 뒤 호출된다 — 다이얼로그 소유는 화면 (규칙 §11-1)
    data object ConfirmEndSession : SessionControlAction

    data object ToggleLock : SessionControlAction
}
