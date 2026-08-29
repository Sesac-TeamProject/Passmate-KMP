package org.sesacteamproject.passmate.payment.data.dto

import kotlinx.serialization.Serializable

// GET /users/me/earnings 응답 — contracts §결제·정산과 1:1
@Serializable
data class EarningsResponse(
    val monthlyTotal: Long = 0,
    val hostSharePercent: Int = 80,
    val nextPayout: NextPayoutDto? = null,
    val paidRoomCount: Int = 0,
    val studentCount: Int = 0,
    val items: List<SettlementItemDto> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false,
    val account: AccountDto? = null
) {

    @Serializable
    data class NextPayoutDto(
        val dateLabel: String = "",
        val amount: Long = 0
    )

    @Serializable
    data class SettlementItemDto(
        val settlementId: Long = 0,
        val dateLabel: String = "",
        val roomTitle: String = "",
        val participantCount: Int = 0,
        val entryFeeTotal: Long = 0,
        val feeAmount: Long = 0,
        val payoutAmount: Long = 0,
        val status: String? = null
    )

    @Serializable
    data class AccountDto(
        val bankName: String = "",
        val maskedNumber: String = "",
        val payoutNote: String? = null
    )
}

// GET/PUT /users/me/settlement-account — contracts §결제·정산과 1:1
@Serializable
data class SettlementAccountDto(
    val bankName: String = "",
    val accountNumber: String = "",
    val holderName: String = ""
)
