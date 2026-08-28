package org.sesacteamproject.passmate.payment.data.remote

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.sesacteamproject.passmate.core.network.ApiClient
import org.sesacteamproject.passmate.payment.data.dto.ChargeCheckoutResponse
import org.sesacteamproject.passmate.payment.data.dto.CoinBalanceResponse
import org.sesacteamproject.passmate.payment.data.dto.CoinTransactionPageResponse
import org.sesacteamproject.passmate.payment.data.dto.ConfirmChargeRequest
import org.sesacteamproject.passmate.payment.data.dto.ConfirmChargeResponse
import org.sesacteamproject.passmate.payment.data.dto.CreateChargeRequest
import org.sesacteamproject.passmate.payment.data.dto.CreateEntryPaymentRequest
import org.sesacteamproject.passmate.payment.data.dto.EntryPaymentResponse
import org.sesacteamproject.passmate.payment.data.dto.PublicRoomPageResponse

// 전송만 담당 — AppResult 변환·매핑은 Repository가 한다 (규칙 §6)
class PaymentRemoteDataSource(
    private val apiClient: ApiClient
) {
    suspend fun fetchMyCoins(): CoinBalanceResponse {
        return apiClient.http.get("${apiClient.baseUrl}/users/me/coins").body()
    }

    suspend fun fetchCoinTransactions(cursor: String?): CoinTransactionPageResponse {
        return apiClient.http.get("${apiClient.baseUrl}/users/me/coins/transactions") {
            if (cursor != null) {
                parameter("cursor", cursor)
            }
        }.body()
    }

    suspend fun createCharge(request: CreateChargeRequest): ChargeCheckoutResponse {
        return apiClient.http.post("${apiClient.baseUrl}/coins/charges") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun confirmCharge(chargeId: String, request: ConfirmChargeRequest): ConfirmChargeResponse {
        return apiClient.http.post("${apiClient.baseUrl}/coins/charges/$chargeId/confirm") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun createEntryPayment(roomId: Long, request: CreateEntryPaymentRequest): EntryPaymentResponse {
        return apiClient.http.post("${apiClient.baseUrl}/rooms/$roomId/entry-payments") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun fetchPublicRooms(
        sort: String,
        query: String?,
        type: String,
        cursor: String?
    ): PublicRoomPageResponse {
        return apiClient.http.get("${apiClient.baseUrl}/rooms/public") {
            parameter("sort", sort)
            parameter("type", type)
            if (!query.isNullOrBlank()) {
                parameter("q", query)
            }
            if (cursor != null) {
                parameter("cursor", cursor)
            }
        }.body()
    }
}
