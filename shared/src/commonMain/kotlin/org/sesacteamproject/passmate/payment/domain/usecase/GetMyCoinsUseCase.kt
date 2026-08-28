package org.sesacteamproject.passmate.payment.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.CoinBalance
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

class GetMyCoinsUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(): AppResult<CoinBalance> {
        return paymentRepository.getMyCoins()
    }
}
