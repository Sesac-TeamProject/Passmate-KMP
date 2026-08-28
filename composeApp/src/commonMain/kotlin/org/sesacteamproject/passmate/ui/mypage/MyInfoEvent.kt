package org.sesacteamproject.passmate.ui.mypage

sealed interface MyInfoEvent {

    // 마이페이지는 회원 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    data object RequireSignIn : MyInfoEvent

    data class OpenReport(val roomId: Long) : MyInfoEvent

    data class Rejoin(val pin: String) : MyInfoEvent

    data class ShowNotice(val message: String) : MyInfoEvent
}
