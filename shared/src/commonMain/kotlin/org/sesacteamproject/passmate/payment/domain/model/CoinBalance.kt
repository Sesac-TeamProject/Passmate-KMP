package org.sesacteamproject.passmate.payment.domain.model

// 내 코인 요약 (GET /users/me/coins) — 보유 코인·기본 결제 수단·최근 내역 1건
data class CoinBalance(
    val balance: Int,
    val defaultMethod: PaymentMethod?,
    val recent: CoinTransaction?
)
