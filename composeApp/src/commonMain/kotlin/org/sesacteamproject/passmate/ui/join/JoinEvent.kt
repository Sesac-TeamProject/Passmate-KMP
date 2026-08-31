package org.sesacteamproject.passmate.ui.join

sealed interface JoinEvent {

    data object RequestQrScan : JoinEvent

    data class JoinCompleted(val pin: String) : JoinEvent

    // 유료 방(회원) — 참가비 결제 화면으로 이동 (US14)
    data class PaymentRequired(val pin: String) : JoinEvent

    data object SignInRequested : JoinEvent

    // 유료 방 게스트 차단·서버 LoginRequired — 로그인 후 결제 화면으로 복귀한다 (스펙 §3)
    data class SignInRequiredForPaidRoom(val pin: String) : JoinEvent

    data class ShowNotice(val message: String) : JoinEvent
}
