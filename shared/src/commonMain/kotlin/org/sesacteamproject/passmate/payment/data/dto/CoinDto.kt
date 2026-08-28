package org.sesacteamproject.passmate.payment.data.dto

import kotlinx.serialization.Serializable

// GET /users/me/coins — 계약 §결제
@Serializable
data class CoinBalanceResponse(
    val balance: Int = 0,
    val defaultMethod: String? = null,
    val recent: CoinTransactionDto? = null
)

// 계약 §결제 CoinTransaction
@Serializable
data class CoinTransactionDto(
    val id: Long = 0,
    val type: String? = null,
    val amount: Int = 0,
    val balanceAfter: Int = 0,
    val method: String? = null,
    val roomTitle: String? = null,
    val paymentNo: String? = null,
    val createdAt: String? = null
)

// GET /users/me/coins/transactions
@Serializable
data class CoinTransactionPageResponse(
    val items: List<CoinTransactionDto> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false
)
