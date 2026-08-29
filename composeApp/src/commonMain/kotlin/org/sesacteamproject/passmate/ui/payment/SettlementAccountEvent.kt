package org.sesacteamproject.passmate.ui.payment

sealed interface SettlementAccountEvent {

    data object Saved : SettlementAccountEvent

    data class ShowNotice(val message: String) : SettlementAccountEvent
}
