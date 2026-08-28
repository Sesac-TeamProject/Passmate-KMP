package org.sesacteamproject.passmate.ui.payment

sealed interface PaymentEvent {

    // 결제·차감·입장 완료 → 대기실로 이동
    data class EnterRoom(val pin: String) : PaymentEvent

    data class ShowNotice(val message: String) : PaymentEvent

    data object SignInRequired : PaymentEvent
}
