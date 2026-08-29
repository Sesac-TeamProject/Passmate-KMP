package org.sesacteamproject.passmate.payment.domain.repository

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
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccount
import org.sesacteamproject.passmate.payment.domain.model.RoomTypeFilter

interface PaymentRepository {

    suspend fun getMyCoins(): AppResult<CoinBalance>

    suspend fun getCoinTransactions(cursor: String?): AppResult<PagedResult<CoinTransaction>>

    // roomId를 넘기면 충전 후 바로 참가비 차감(원스텝 입장) 대상 방을 지정한다
    suspend fun requestCharge(amount: Int, method: PaymentMethod, roomId: Long?): AppResult<CoinCheckout>

    suspend fun confirmCharge(chargeId: String, paymentId: String, roomId: Long?): AppResult<ChargeConfirm>

    suspend fun payEntryFee(roomId: Long, nickname: String, avatarId: Int?): AppResult<EntryPayment>

    suspend fun getPublicRooms(
        sort: RoomSort,
        query: String?,
        type: RoomTypeFilter,
        cursor: String?
    ): AppResult<PagedResult<PublicRoom>>

    // ── 정산 (M-T4·M-12-3, FR-056) — 금액 계산(8:2·수수료)은 전부 서버가 한다 ──

    suspend fun getEarnings(cursor: String?): AppResult<Earnings>

    // 미등록이면 404 NotFound — 화면은 빈 폼으로 처리한다
    suspend fun getSettlementAccount(): AppResult<SettlementAccount>

    suspend fun saveSettlementAccount(account: SettlementAccount): AppResult<Unit>

    // 기본 결제 수단 설정 (M-12-8, PUT /users/me/payment-method)
    suspend fun setDefaultPaymentMethod(method: PaymentMethod): AppResult<Unit>
}
