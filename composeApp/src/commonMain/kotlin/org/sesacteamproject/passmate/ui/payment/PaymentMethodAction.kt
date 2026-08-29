package org.sesacteamproject.passmate.ui.payment

import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod

sealed interface PaymentMethodAction {

    data object Enter : PaymentMethodAction

    data class Select(val method: PaymentMethod) : PaymentMethodAction

    data object Submit : PaymentMethodAction
}
