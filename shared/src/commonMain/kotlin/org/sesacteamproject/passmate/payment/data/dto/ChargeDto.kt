package org.sesacteamproject.passmate.payment.data.dto

import kotlinx.serialization.Serializable

// POST /coins/charges — roomId 있으면 충전 후 바로 차감할 방
@Serializable
data class CreateChargeRequest(
    val amount: Int,
    val method: String,
    val roomId: Long? = null
)

// POST /coins/charges 응답 — 포트원 결제창 파라미터
@Serializable
data class ChargeCheckoutResponse(
    val chargeId: String = "",
    val storeId: String = "",
    val channelKey: String = "",
    val paymentId: String = "",
    val orderName: String = "",
    val amount: Int = 0,
    val currency: String = "KRW",
    val payMethod: String = "EASY_PAY"
)

// POST /coins/charges/{chargeId}/confirm — 포트원 결제 결과 paymentId(imp_uid) 전달
@Serializable
data class ConfirmChargeRequest(
    val paymentId: String,
    val roomId: Long? = null
)

@Serializable
data class ConfirmChargeResponse(
    val balance: Int = 0,
    val entryPayment: EntryPaymentResponse? = null
)
