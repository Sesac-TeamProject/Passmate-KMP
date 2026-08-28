package org.sesacteamproject.passmate.ui.join

sealed interface JoinEvent {

    data object RequestQrScan : JoinEvent

    data class JoinCompleted(val pin: String) : JoinEvent

    // 유료 방(회원) — 참가비 결제 화면으로 이동 (US14)
    data class PaymentRequired(val pin: String) : JoinEvent

    data object SignInRequested : JoinEvent

    data class ShowNotice(val message: String) : JoinEvent
}
