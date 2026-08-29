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

    // PTT 녹음 종료 → 클립 업로드 (M-T2 "길게 눌러 힌트 말하기", T121)
    data class SendVoiceHint(val hint: RecordedVoiceHint) : SessionControlAction

    data class Notice(val message: String) : SessionControlAction
}
