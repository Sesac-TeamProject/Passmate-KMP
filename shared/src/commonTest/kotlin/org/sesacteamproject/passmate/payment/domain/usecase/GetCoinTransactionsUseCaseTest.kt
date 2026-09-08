package org.sesacteamproject.passmate.payment.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.sesacteamproject.passmate.core.model.AppError
import org.sesacteamproject.passmate.core.model.AppResult
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.payment.domain.model.ChargeConfirm
import org.sesacteamproject.passmate.payment.domain.model.CoinBalance
import org.sesacteamproject.passmate.payment.domain.model.CoinCheckout
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.model.CoinTransactionType
import org.sesacteamproject.passmate.payment.domain.model.Earnings
import org.sesacteamproject.passmate.payment.domain.model.EntryPayment
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.model.PublicRoom
import org.sesacteamproject.passmate.payment.domain.model.RoomSort
import org.sesacteamproject.passmate.payment.domain.model.RoomTypeFilter
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccount
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository

private class FakePaymentRepository(
    private val page: PagedResult<CoinTransaction>
) : PaymentRepository {

    override suspend fun getMyCoins(): AppResult<CoinBalance> = AppResult.Failure(AppError.NotFound())

    override suspend fun getCoinTransactions(cursor: String?): AppResult<PagedResult<CoinTransaction>> = AppResult.Success(page)

    override suspend fun requestCharge(amount: Int, method: PaymentMethod, roomId: Long?): AppResult<CoinCheckout> = AppResult.Failure(AppError.NotFound())

    override suspend fun confirmCharge(chargeId: String, paymentId: String, roomId: Long?): AppResult<ChargeConfirm> = AppResult.Failure(AppError.NotFound())

    override suspend fun payEntryFee(roomId: Long, nickname: String, avatarId: Int?): AppResult<EntryPayment> = AppResult.Failure(AppError.NotFound())

    override suspend fun getPublicRooms(
        sort: RoomSort,
        query: String?,
        type: RoomTypeFilter,
        cursor: String?
    ): AppResult<PagedResult<PublicRoom>> = AppResult.Failure(AppError.NotFound())

    override suspend fun getEarnings(cursor: String?): AppResult<Earnings> = AppResult.Failure(AppError.NotFound())

    override suspend fun getSettlementAccount(): AppResult<SettlementAccount> = AppResult.Failure(AppError.NotFound())

    override suspend fun saveSettlementAccount(account: SettlementAccount): AppResult<Unit> = AppResult.Failure(AppError.NotFound())

    override suspend fun setDefaultPaymentMethod(method: PaymentMethod): AppResult<Unit> = AppResult.Failure(AppError.NotFound())
}

private fun transaction(id: Long, createdAt: String?): CoinTransaction {
    return CoinTransaction(
        id = id,
        type = CoinTransactionType.CHARGE,
        amount = 1000,
        balanceAfter = 1000,
        method = PaymentMethod.KAKAO_PAY,
        roomTitle = null,
        paymentNo = null,
        createdAt = createdAt
    )
}

// 시안 M-12-9 코인 내역은 최신순(8/22 → 8/01)이다. 서버가 어느 순서로 주든 화면은 최신이 위여야 한다
class GetCoinTransactionsUseCaseTest {

    @Test
    fun ordersTransactionsNewestFirstRegardlessOfServerOrder() = runTest {
        val page = PagedResult(
            items = listOf(
                transaction(1, "2026-08-10T10:00:00Z"),
                transaction(2, "2026-08-22T10:00:00Z"),
                transaction(3, "2026-08-15T10:00:00Z")
            ),
            nextCursor = "c2",
            hasNext = true
        )
        val useCase = GetCoinTransactionsUseCase(FakePaymentRepository(page))

        val result = useCase.invoke(cursor = null)

        val items = (result as AppResult.Success).value.items
        assertEquals(listOf(2L, 3L, 1L), items.map { it.id })
        // 페이징 정보는 그대로 지나간다
        assertEquals("c2", result.value.nextCursor)
        assertEquals(true, result.value.hasNext)
    }

    @Test
    fun putsTransactionsWithoutTimestampLast() = runTest {
        val page = PagedResult(
            items = listOf(
                transaction(1, null),
                transaction(2, "2026-08-22T10:00:00Z")
            ),
            nextCursor = null,
            hasNext = false
        )
        val useCase = GetCoinTransactionsUseCase(FakePaymentRepository(page))

        val result = useCase.invoke(cursor = null)

        val items = (result as AppResult.Success).value.items
        assertEquals(listOf(2L, 1L), items.map { it.id })
    }
}
