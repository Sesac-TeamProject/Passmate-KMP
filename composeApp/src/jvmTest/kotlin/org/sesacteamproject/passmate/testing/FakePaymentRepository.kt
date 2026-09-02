package org.sesacteamproject.passmate.testing

import kotlinx.coroutines.CompletableDeferred
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.payment.domain.model.ChargeConfirm
import org.sesacteamproject.passmate.payment.domain.model.CoinBalance
import org.sesacteamproject.passmate.payment.domain.model.CoinCheckout
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.model.Earnings
import org.sesacteamproject.passmate.payment.domain.model.EntryPayment
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.model.PublicRoom
import org.sesacteamproject.passmate.payment.domain.model.RoomSort
import org.sesacteamproject.passmate.payment.domain.model.RoomTypeFilter
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccount
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

class FakePaymentRepository(
    var coinsResult: AppResult<CoinBalance> = AppResult.Failure(AppError.Unknown()),
    var earningsResult: AppResult<Earnings> = AppResult.Failure(AppError.Unknown()),
    var chargeResult: AppResult<CoinCheckout> = AppResult.Failure(AppError.Unknown()),
    var confirmResult: AppResult<ChargeConfirm> = AppResult.Failure(AppError.Unknown())
) : PaymentRepository {

    // 응답을 붙잡아 두는 게이트 — in-flight 가드 테스트용. null이면 즉시 반환한다
    var coinsGate: CompletableDeferred<Unit>? = null

    var coinsCalls: Int = 0

    var earningsCalls: Int = 0

    var chargedAmount: Int? = null

    var chargedMethod: PaymentMethod? = null

    var confirmedPaymentId: String? = null

    override suspend fun getMyCoins(): AppResult<CoinBalance> {
        coinsCalls += 1
        coinsGate?.await()
        return coinsResult
    }

    override suspend fun getCoinTransactions(cursor: String?): AppResult<PagedResult<CoinTransaction>> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun requestCharge(amount: Int, method: PaymentMethod, roomId: Long?): AppResult<CoinCheckout> {
        chargedAmount = amount
        chargedMethod = method
        return chargeResult
    }

    override suspend fun confirmCharge(chargeId: String, paymentId: String, roomId: Long?): AppResult<ChargeConfirm> {
        confirmedPaymentId = paymentId
        return confirmResult
    }

    override suspend fun payEntryFee(roomId: Long, nickname: String, avatarId: Int?): AppResult<EntryPayment> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun getPublicRooms(
        sort: RoomSort,
        query: String?,
        type: RoomTypeFilter,
        cursor: String?
    ): AppResult<PagedResult<PublicRoom>> {
        return AppResult.Failure(AppError.Unknown())
    }

    override suspend fun getEarnings(cursor: String?): AppResult<Earnings> {
        earningsCalls += 1
        return earningsResult
    }

    override suspend fun getSettlementAccount(): AppResult<SettlementAccount> {
        return AppResult.Failure(AppError.NotFound())
    }

    override suspend fun saveSettlementAccount(account: SettlementAccount): AppResult<Unit> {
        return AppResult.Success(Unit)
    }

    override suspend fun setDefaultPaymentMethod(method: PaymentMethod): AppResult<Unit> {
        return AppResult.Success(Unit)
    }
}
