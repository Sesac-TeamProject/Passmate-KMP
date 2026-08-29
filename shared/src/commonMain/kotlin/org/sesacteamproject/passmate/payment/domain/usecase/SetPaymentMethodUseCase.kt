package org.sesacteamproject.passmate.payment.domain.usecase

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

// 기본 결제 수단 설정 (M-12-8) — 카드 정보는 저장하지 않는다(포트원 처리)
class SetPaymentMethodUseCase(
    private val paymentRepository: PaymentRepository
) {
    suspend operator fun invoke(method: PaymentMethod): AppResult<Unit> {
        return paymentRepository.setDefaultPaymentMethod(method)
    }
}
