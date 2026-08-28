package org.sesacteamproject.passmate.payment.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.ChargeConfirm
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

class ConfirmChargeUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(chargeId: String, paymentId: String, roomId: Long? = null): AppResult<ChargeConfirm> {
        return paymentRepository.confirmCharge(chargeId, paymentId, roomId)
    }
}
