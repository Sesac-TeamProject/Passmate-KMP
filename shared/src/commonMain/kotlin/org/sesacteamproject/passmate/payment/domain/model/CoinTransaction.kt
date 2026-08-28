package org.sesacteamproject.passmate.payment.domain.model

// 코인 내역 1건 — 충전(+)·차감(-)·환급(+) (계약 §결제 CoinTransaction)
// amount는 부호 포함(충전/환급 양수, 차감 음수), balanceAfter는 건별 잔액
data class CoinTransaction(
    val id: Long,
    val type: CoinTransactionType,
    val amount: Int,
    val balanceAfter: Int,
    val method: PaymentMethod?,
    val roomTitle: String?,
    val paymentNo: String?,
    val createdAt: String?
)

enum class CoinTransactionType(val wireValue: String) {

    CHARGE("CHARGE"),
    DEDUCT("DEDUCT"),
    REFUND("REFUND");

    companion object {

        fun from(wireValue: String?): CoinTransactionType {
            return entries.firstOrNull { it.wireValue == wireValue } ?: CHARGE
        }
    }
}
