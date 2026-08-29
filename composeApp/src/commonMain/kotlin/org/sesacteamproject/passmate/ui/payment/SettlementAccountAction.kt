package org.sesacteamproject.passmate.ui.payment

sealed interface SettlementAccountAction {

    data object Enter : SettlementAccountAction

    data class ChangeBankName(val text: String) : SettlementAccountAction

    data class ChangeAccountNumber(val text: String) : SettlementAccountAction

    data class ChangeHolderName(val text: String) : SettlementAccountAction

    data object Submit : SettlementAccountAction
}
