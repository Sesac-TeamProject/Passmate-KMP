package org.sesacteamproject.passmate.payment.domain.model

// 코인 충전 요청(POST /coins/charges) 응답 — 포트원 결제창 호출 파라미터.
// 클라이언트는 이 값으로 PortOne.requestPayment(...)를 호출하고, 결과 paymentId를 confirm에 전달한다.
data class CoinCheckout(
    val chargeId: String,
    val storeId: String,
    val channelKey: String,
    val paymentId: String,
    val orderName: String,
    val amount: Int,
    val currency: String,
    val payMethod: String
)
