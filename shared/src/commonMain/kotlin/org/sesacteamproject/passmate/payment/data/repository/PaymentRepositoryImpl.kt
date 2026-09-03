package org.sesacteamproject.passmate.payment.data.repository

import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.core.model.map
import org.sesacteamproject.passmate.core.network.apiCall
import org.sesacteamproject.passmate.payment.data.dto.ConfirmChargeRequest
import org.sesacteamproject.passmate.payment.data.dto.CreateChargeRequest
import org.sesacteamproject.passmate.payment.data.dto.CreateEntryPaymentRequest
import org.sesacteamproject.passmate.payment.data.dto.PaymentMethodRequest
import org.sesacteamproject.passmate.payment.data.dto.SettlementAccountDto
import org.sesacteamproject.passmate.payment.data.mapper.toDomain
import org.sesacteamproject.passmate.payment.data.mapper.toSummary
import org.sesacteamproject.passmate.payment.data.remote.PaymentRemoteDataSource
import org.sesacteamproject.passmate.payment.domain.model.ChargeConfirm
import org.sesacteamproject.passmate.payment.domain.model.CoinBalance
import org.sesacteamproject.passmate.payment.domain.model.CoinCheckout
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.model.Earnings
import org.sesacteamproject.passmate.payment.domain.model.EntryPayment
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.model.PublicRoom
import org.sesacteamproject.passmate.payment.domain.model.RoomSort
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccount
import org.sesacteamproject.passmate.payment.domain.model.RoomTypeFilter
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

class PaymentRepositoryImpl(
    private val remoteDataSource: PaymentRemoteDataSource
) : PaymentRepository {

    override suspend fun getMyCoins(): AppResult<CoinBalance> {
        return apiCall { remoteDataSource.fetchMyCoins() }.map { it.toDomain() }
    }

    override suspend fun getCoinTransactions(cursor: String?): AppResult<PagedResult<CoinTransaction>> {
        return apiCall { remoteDataSource.fetchCoinTransactions(cursor) }.map { it.toDomain() }
    }

    override suspend fun requestCharge(amount: Int, method: PaymentMethod, roomId: Long?): AppResult<CoinCheckout> {
        val request = CreateChargeRequest(amount = amount, method = method.wireValue, roomId = roomId)

        return apiCall { remoteDataSource.createCharge(request) }.map { it.toDomain() }
    }

    override suspend fun confirmCharge(chargeId: String, paymentId: String, roomId: Long?): AppResult<ChargeConfirm> {
        val request = ConfirmChargeRequest(paymentId = paymentId, roomId = roomId)

        return apiCall { remoteDataSource.confirmCharge(chargeId, request) }.map { it.toDomain() }
    }

    override suspend fun payEntryFee(roomId: Long, nickname: String, avatarId: Int?): AppResult<EntryPayment> {
        val request = CreateEntryPaymentRequest(nickname = nickname, avatarId = avatarId)

        return apiCall { remoteDataSource.createEntryPayment(roomId, request) }.map { it.toDomain() }
    }

    override suspend fun getPublicRooms(
        sort: RoomSort,
        query: String?,
        type: RoomTypeFilter,
        cursor: String?
    ): AppResult<PagedResult<PublicRoom>> {
        return apiCall {
            remoteDataSource.fetchPublicRooms(sort.wireValue, query, type.wireValue, cursor)
        }.map { it.toDomain() }
    }

    // 정산 화면은 금액과 계좌를 함께 그린다. 계좌 은행 정보가 earnings 응답에 없어
    // 여기서 두 엔드포인트를 합성한다 (규칙 §6 — 화면 전용 집계는 Repository 책임).
    override suspend fun getEarnings(cursor: String?): AppResult<Earnings> {
        val accountResult = apiCall { remoteDataSource.fetchSettlementAccount() }
        val account = (accountResult as? AppResult.Success)?.value?.toSummary()

        return apiCall { remoteDataSource.fetchEarnings() }.map { it.toDomain(account) }
    }

    override suspend fun getSettlementAccount(): AppResult<SettlementAccount> {
        return apiCall { remoteDataSource.fetchSettlementAccount() }.map { it.toDomain() }
    }

    override suspend fun saveSettlementAccount(account: SettlementAccount): AppResult<Unit> {
        val request = SettlementAccountDto(
            bankName = account.bankName.trim(),
            accountNo = account.accountNumber.trim(),
            holderName = account.holderName.trim()
        )

        return apiCall { remoteDataSource.putSettlementAccount(request) }
    }

    override suspend fun setDefaultPaymentMethod(method: PaymentMethod): AppResult<Unit> {
        return apiCall { remoteDataSource.putPaymentMethod(PaymentMethodRequest(method.wireValue)) }
    }
}
