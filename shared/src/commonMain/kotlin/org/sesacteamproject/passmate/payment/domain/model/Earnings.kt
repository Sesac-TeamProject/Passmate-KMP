package org.sesacteamproject.passmate.payment.domain.model

// 수익·정산 요약 (GET /users/me/earnings) — M-T4 (FR-056, 8:2 정산)
data class Earnings(
    val monthlyTotal: Long,
    val hostSharePercent: Int,
    val nextPayout: NextPayout?,
    val paidRoomCount: Int,
    val studentCount: Int,
    val items: List<SettlementItem>,
    val nextCursor: String?,
    val hasNext: Boolean,
    val account: SettlementAccountSummary?
)

data class NextPayout(
    val dateLabel: String,
    val amount: Long
)

// 정산 내역 1건 — 상태 칩: 정산 예정(SCHEDULED)·지급 완료(PAID)·보류(HELD)
data class SettlementItem(
    val settlementId: Long,
    val dateLabel: String,
    val roomTitle: String,
    val participantCount: Int,
    val entryFeeTotal: Long,
    val feeAmount: Long,
    val payoutAmount: Long,
    val status: SettlementStatus
)

enum class SettlementStatus {
    SCHEDULED,
    PAID,
    HELD,
    UNKNOWN;

    companion object {

        fun from(raw: String?): SettlementStatus {
            return when (raw?.uppercase()) {
                "SCHEDULED" -> SCHEDULED
                "PAID" -> PAID
                "HELD" -> HELD
                else -> UNKNOWN
            }
        }
    }
}

// 정산 화면 하단 계좌 요약 — 상세·수정은 정산 계좌 API 사용
data class SettlementAccountSummary(
    val bankName: String,
    val maskedNumber: String,
    val payoutNote: String?
)

// 정산 계좌 (GET/PUT /users/me/settlement-account) — M-12-3
data class SettlementAccount(
    val bankName: String,
    val accountNumber: String,
    val holderName: String
)
