package org.sesacteamproject.passmate.payment.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.CoinCheckout
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

class RequestChargeUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(amount: Int, roomId: Long? = null): AppResult<CoinCheckout> {
        return paymentRepository.requestCharge(amount, roomId)
    }
}
