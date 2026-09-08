package org.sesacteamproject.passmate.payment.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

class GetCoinTransactionsUseCase(
    private val paymentRepository: PaymentRepository
) {
    // 시안 M-12-9는 최신순이다. 서버 순서에 기대지 않고 페이지 안에서 createdAt 내림차순으로 맞춘다 —
    // ISO 8601 문자열이라 사전순 비교가 시간순이다. 시각이 없는 건은 맨 뒤로
    private fun PagedResult<CoinTransaction>.newestFirst(): PagedResult<CoinTransaction> {
        val sorted = items.sortedWith(compareByDescending<CoinTransaction> { it.createdAt != null }.thenByDescending { it.createdAt })

        return copy(items = sorted)
    }

    suspend operator fun invoke(cursor: String? = null): AppResult<PagedResult<CoinTransaction>> {
        val result = paymentRepository.getCoinTransactions(cursor)

        return when (result) {
            is AppResult.Success -> AppResult.Success(result.value.newestFirst())
            is AppResult.Failure -> result
        }
    }
}
