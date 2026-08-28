package org.sesacteamproject.passmate.payment.domain.model

// 코인 충전 승인 검증(POST /coins/charges/{chargeId}/confirm) 결과.
// roomId를 함께 넘겨 "충전 → 차감하고 입장"을 한 번에 처리한 경우 entryPayment가 채워진다.
data class ChargeConfirm(
    val balance: Int,
    val entryPayment: EntryPayment?
)
