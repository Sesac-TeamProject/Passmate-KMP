package org.sesacteamproject.passmate.payment.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccount
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

class SaveSettlementAccountUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(account: SettlementAccount): AppResult<Unit> {
        return paymentRepository.saveSettlementAccount(account)
    }
}
