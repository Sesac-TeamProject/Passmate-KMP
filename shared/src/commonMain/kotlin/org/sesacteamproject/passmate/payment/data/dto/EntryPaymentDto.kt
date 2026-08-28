package org.sesacteamproject.passmate.payment.data.dto

import kotlinx.serialization.Serializable

// POST /rooms/{roomId}/entry-payments — 참가비 코인 차감(닉네임·캐릭터 함께 전달)
@Serializable
data class CreateEntryPaymentRequest(
    val nickname: String,
    val avatarId: Int? = null
)

// 잔액 충분 시 응답. 부족(402)은 ErrorResponse로 처리하고 부족분은 CoinPolicy로 계산한다.
@Serializable
data class EntryPaymentResponse(
    val paymentNo: String = "",
    val balance: Int = 0
)
