package org.sesacteamproject.passmate.payment.data.dto

import kotlinx.serialization.Serializable

// POST /rooms/{roomId}/entry-payments — 참가비 코인 차감(닉네임·캐릭터 함께 전달).
// 캐릭터는 서버 규격대로 문자열 키로 보낸다 (ERD avatar_id varchar(30))
@Serializable
data class CreateEntryPaymentRequest(
    val nickname: String,
    val avatarId: String? = null
)

// 잔액 충분 시 응답. 부족(402)은 ErrorResponse로 처리하고 부족분은 CoinPolicy로 계산한다.
// 서버는 차감 후 잔액을 balanceAfter로 준다(백엔드 `EntryPaymentResponse`, 2026-09-07 확인) — 계약 갱신 대상
@Serializable
data class EntryPaymentResponse(
    val paymentNo: String = "",
    val balanceAfter: Int = 0
)
