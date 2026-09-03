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

// 정산 계좌 (GET/PUT /users/me/settlement-account) — M-12-3.
// 조회는 마스킹된 번호만 준다(계약 `SettlementAccountView.accountNoMasked`) —
// 그대로 다시 저장하면 실제 계좌번호가 마스킹 문자열로 덮인다. 저장은 별도 입력값으로 한다.
data class SettlementAccount(
    val bankName: String,
    val maskedAccountNumber: String,
    val holderName: String
)
