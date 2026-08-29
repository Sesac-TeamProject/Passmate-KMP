package org.sesacteamproject.passmate.payment.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccount
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

// 미등록이면 404 NotFound — 화면은 빈 폼으로 처리한다 (M-12-3)
class GetSettlementAccountUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(): AppResult<SettlementAccount> {
        return paymentRepository.getSettlementAccount()
    }
}
