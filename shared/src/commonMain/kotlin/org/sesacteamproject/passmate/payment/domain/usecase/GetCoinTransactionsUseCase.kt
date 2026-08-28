package org.sesacteamproject.passmate.payment.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

class GetCoinTransactionsUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(cursor: String? = null): AppResult<PagedResult<CoinTransaction>> {
        return paymentRepository.getCoinTransactions(cursor)
    }
}
