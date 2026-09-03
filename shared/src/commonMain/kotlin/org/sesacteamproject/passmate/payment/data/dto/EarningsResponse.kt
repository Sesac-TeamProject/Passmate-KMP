package org.sesacteamproject.passmate.payment.data.dto

import kotlinx.serialization.Serializable

// GET /users/me/earnings 응답 — 계약 `HostEarningsResponse`와 1:1.
// 페이징이 없다(전량 반환). 계좌 은행 정보는 이 응답에 없고 registered 여부만 온다.
@Serializable
data class EarningsResponse(
    val thisMonthNet: Long = 0,
    // 다음 지급 예정 금액
    val pendingNet: Long = 0,
    // LocalDate 직렬화 — "2026-09-05"
    val nextPayoutDate: String? = null,
    val accountRegistered: Boolean = false,
    val earnings: List<EarningRowDto> = emptyList()
) {

    @Serializable
    data class EarningRowDto(
        val roomId: Long = 0,
        val roomTitle: String = "",
        val participantCount: Int = 0,
        // 참가비 총액
        val gross: Long = 0,
        // 플랫폼 수수료
        val platformFee: Long = 0,
        // 실지급액
        val net: Long = 0,
        val status: String? = null,
        // LocalDateTime 직렬화 — "2026-08-22T21:10:00"
        val earnedAt: String? = null
    )
}

// GET /users/me/settlement-account 응답 — 계약 `SettlementAccountResponse`와 1:1
@Serializable
data class SettlementAccountResponse(
    val registered: Boolean = false,
    val account: SettlementAccountView? = null
)

// 계약 `SettlementAccountView` — 조회는 마스킹된 계좌번호만 준다
@Serializable
data class SettlementAccountView(
    val bankCode: String? = null,
    val bankName: String = "",
    val accountNoMasked: String = "",
    val holderName: String = "",
    val verified: Boolean = false
)

// PUT /users/me/settlement-account 요청 — 계약 `SettlementAccountRequest`
@Serializable
data class SettlementAccountDto(
    val bankCode: String? = null,
    val bankName: String = "",
    val accountNo: String = "",
    val holderName: String = ""
)

// PUT /users/me/payment-method — 기본 결제 수단 (M-12-8)
@Serializable
data class PaymentMethodRequest(
    val method: String
)
