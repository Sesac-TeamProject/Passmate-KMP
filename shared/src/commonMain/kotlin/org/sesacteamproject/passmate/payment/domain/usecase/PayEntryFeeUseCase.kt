package org.sesacteamproject.passmate.payment.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.EntryPayment
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

class PayEntryFeeUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(roomId: Long, nickname: String, avatarId: Int? = null): AppResult<EntryPayment> {
        return paymentRepository.payEntryFee(roomId, nickname, avatarId)
    }
}
