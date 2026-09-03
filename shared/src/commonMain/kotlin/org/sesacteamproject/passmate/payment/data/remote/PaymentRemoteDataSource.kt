package org.sesacteamproject.passmate.payment.data.remote

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import org.sesacteamproject.passmate.payment.data.dto.EarningsResponse
import org.sesacteamproject.passmate.payment.data.dto.PaymentMethodRequest
import org.sesacteamproject.passmate.payment.data.dto.SettlementAccountDto
import org.sesacteamproject.passmate.payment.data.dto.SettlementAccountResponse
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

    // 서버는 page/size 기반이다. cursor 자리에는 다음 페이지 번호가 실려 온다 (Mapper 참고).
    // type=ALL은 서버 enum(FREE·PAID)에 없으므로 파라미터를 보내지 않는다.
    suspend fun fetchPublicRooms(
        sort: String,
        query: String?,
        type: String?,
        cursor: String?
    ): PublicRoomPageResponse {
        return apiClient.http.get("${apiClient.baseUrl}/rooms/public") {
            parameter("sort", sort)
            if (type != null) {
                parameter("type", type)
            }
            if (!query.isNullOrBlank()) {
                parameter("q", query)
            }
            if (cursor != null) {
                parameter("page", cursor)
            }
        }.body()
    }

    // 서버는 정산 내역을 페이징하지 않고 전량 반환한다
    suspend fun fetchEarnings(): EarningsResponse {
        return apiClient.http.get("${apiClient.baseUrl}/users/me/earnings").body()
    }

    suspend fun fetchSettlementAccount(): SettlementAccountResponse {
        return apiClient.http.get("${apiClient.baseUrl}/users/me/settlement-account").body()
    }

    suspend fun putPaymentMethod(request: PaymentMethodRequest) {
        apiClient.http.put("${apiClient.baseUrl}/users/me/payment-method") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun putSettlementAccount(request: SettlementAccountDto) {
        apiClient.http.put("${apiClient.baseUrl}/users/me/settlement-account") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
