package org.sesacteamproject.passmate.payment.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.Earnings
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

class GetEarningsUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(cursor: String?): AppResult<Earnings> {
        return paymentRepository.getEarnings(cursor)
    }
}
