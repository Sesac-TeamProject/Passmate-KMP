package org.sesacteamproject.passmate.ui.payment

sealed interface PaymentMethodEvent {

    data object Saved : PaymentMethodEvent

    data class ShowNotice(val message: String) : PaymentMethodEvent
}
