package org.sesacteamproject.passmate.ui.payment

import org.sesacteamproject.passmate.payment.domain.model.Bank

sealed interface SettlementAccountAction {

    data object Enter : SettlementAccountAction

    data class SelectBank(val bank: Bank) : SettlementAccountAction

    data class ChangeAccountNumber(val text: String) : SettlementAccountAction

    data class ChangeHolderName(val text: String) : SettlementAccountAction

    data object Submit : SettlementAccountAction
}
