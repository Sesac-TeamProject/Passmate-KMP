package org.sesacteamproject.passmate.payment.domain.model

// 참가비 코인 차감 성공 결과 — 결제 번호(PM-YYYY-MMDD-NNNN)와 남은 코인, 입장 자격 부여.
// 잔액 부족은 AppError.PaymentRequired로 반환하고 부족분은 CoinPolicy로 계산한다.
data class EntryPayment(
    val paymentNo: String,
    val balance: Int
)
