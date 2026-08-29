package org.sesacteamproject.passmate.ui.hostroom

sealed interface SessionControlEvent {

    // 리모컨은 호스트(회원) 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    data object RequireSignIn : SessionControlEvent

    // SESSION_ENDED 수신 → 방 리포트로 이동 (세션 플로우 전환은 서버 이벤트로만, 규칙 §2-1-2)
    data class SessionEnded(val roomId: Long) : SessionControlEvent

    data class ShowNotice(val message: String) : SessionControlEvent
}
