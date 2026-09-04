package org.sesacteamproject.passmate.payment.data.mapper

import org.sesacteamproject.passmate.core.model.DisplayDate
import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.payment.data.dto.ChargeCheckoutResponse
import org.sesacteamproject.passmate.payment.data.dto.CoinBalanceResponse
import org.sesacteamproject.passmate.payment.data.dto.EarningsResponse
import org.sesacteamproject.passmate.payment.data.dto.SettlementAccountResponse
import org.sesacteamproject.passmate.payment.data.dto.CoinTransactionDto
import org.sesacteamproject.passmate.payment.data.dto.CoinTransactionPageResponse
import org.sesacteamproject.passmate.payment.data.dto.ConfirmChargeResponse
import org.sesacteamproject.passmate.payment.data.dto.EntryPaymentResponse
import org.sesacteamproject.passmate.payment.data.dto.PublicRoomDto
import org.sesacteamproject.passmate.payment.data.dto.PublicRoomPageResponse
import org.sesacteamproject.passmate.payment.domain.model.ChargeConfirm
import org.sesacteamproject.passmate.payment.domain.model.CoinBalance
import org.sesacteamproject.passmate.payment.domain.model.Earnings
import org.sesacteamproject.passmate.payment.domain.model.CoinCheckout
import org.sesacteamproject.passmate.payment.domain.model.CoinTransaction
import org.sesacteamproject.passmate.payment.domain.model.CoinTransactionType
import org.sesacteamproject.passmate.payment.domain.model.EntryPayment
import org.sesacteamproject.passmate.payment.domain.model.NextPayout
import org.sesacteamproject.passmate.payment.domain.model.PaymentMethod
import org.sesacteamproject.passmate.payment.domain.model.PublicRoom
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccount
import org.sesacteamproject.passmate.payment.domain.model.SettlementAccountSummary
import org.sesacteamproject.passmate.payment.domain.model.SettlementItem
import org.sesacteamproject.passmate.payment.domain.model.SettlementStatus
import org.sesacteamproject.passmate.payment.domain.policy.SettlementPolicy
import org.sesacteamproject.passmate.room.domain.model.RoomStatus

fun CoinBalanceResponse.toDomain(): CoinBalance {
    return CoinBalance(
        balance = balance,
        defaultMethod = PaymentMethod.from(defaultMethod),
        recent = recent?.toDomain()
    )
}

fun CoinTransactionDto.toDomain(): CoinTransaction {
    return CoinTransaction(
        id = id,
        type = CoinTransactionType.from(type),
        amount = amount,
        balanceAfter = balanceAfter,
        method = PaymentMethod.from(method),
        roomTitle = roomTitle,
        paymentNo = paymentNo,
        createdAt = createdAt
    )
}

fun CoinTransactionPageResponse.toDomain(): PagedResult<CoinTransaction> {
    return PagedResult(
        items = items.map { it.toDomain() },
        nextCursor = nextCursor,
        hasNext = hasNext
    )
}

fun ChargeCheckoutResponse.toDomain(): CoinCheckout {
    return CoinCheckout(
        chargeId = chargeId,
        storeId = storeId,
        channelKey = channelKey,
        paymentId = paymentId,
        orderName = orderName,
        amount = amount,
        currency = currency,
        payMethod = payMethod
    )
}

fun EntryPaymentResponse.toDomain(): EntryPayment {
    return EntryPayment(
        paymentNo = paymentNo,
        balance = balance
    )
}

fun ConfirmChargeResponse.toDomain(): ChargeConfirm {
    return ChargeConfirm(
        balance = balance,
        entryPayment = entryPayment?.toDomain()
    )
}

fun PublicRoomDto.toDomain(): PublicRoom {
    return PublicRoom(
        roomId = id,
        title = title,
        topic = topic,
        hostId = host?.userId,
        hostName = host?.nickname ?: "",
        // 서버가 등급·별점을 주지 않는다 (계약 `PublicRoomHostResponse` 참고)
        hostLevel = null,
        hostRating = null,
        status = RoomStatus.from(status),
        participantCount = participantCount,
        maxParticipants = maxParticipants,
        isPaid = type.equals("PAID", ignoreCase = true),
        entryFee = fee,
        scheduledAt = scheduledAt
    )
}

// 서버는 page/size 기반이라 커서가 없다. 다음 페이지 번호를 커서 자리에 실어
// 도메인 `PagedResult` 계약(§6)을 그대로 유지한다.
fun PublicRoomPageResponse.toDomain(): PagedResult<PublicRoom> {
    return PagedResult(
        items = content.map { it.toDomain() },
        nextCursor = if (hasNext) (page + 1).toString() else null,
        hasNext = hasNext
    )
}

// 서버는 hostSharePercent를 주지 않는다 — 8:2 정산(FR-056)은 SettlementPolicy가 단일 출처다.
// earnings가 페이징 없이 전량 오므로 방 수·학생 수 집계는 정확하다.
fun EarningsResponse.toDomain(account: SettlementAccountSummary?): Earnings {
    return Earnings(
        monthlyTotal = thisMonthNet,
        hostSharePercent = SettlementPolicy.hostSharePercent,
        nextPayout = nextPayoutDate?.let { NextPayout(dateLabel = DisplayDate.format(it) ?: it, amount = pendingNet) },
        paidRoomCount = earnings.size,
        studentCount = earnings.sumOf { it.participantCount },
        items = earnings.map { it.toDomain() },
        nextCursor = null,
        hasNext = false,
        account = account
    )
}

fun EarningsResponse.EarningRowDto.toDomain(): SettlementItem {
    return SettlementItem(
        settlementId = roomId,
        dateLabel = DisplayDate.format(earnedAt) ?: "",
        roomTitle = roomTitle,
        participantCount = participantCount,
        entryFeeTotal = gross,
        feeAmount = platformFee,
        payoutAmount = net,
        status = SettlementStatus.from(status)
    )
}

// 계좌 미등록이면 null — 정산 화면의 "계좌 등록 먼저" 빈 상태(M-T4)가 이 값으로 갈린다
fun SettlementAccountResponse.toSummary(): SettlementAccountSummary? {
    val view = account

    return if (registered && view != null) {
        SettlementAccountSummary(
            bankName = view.bankName,
            maskedNumber = view.accountNoMasked,
            payoutNote = null
        )
    } else {
        null
    }
}

fun SettlementAccountResponse.toDomain(): SettlementAccount {
    val view = account

    return SettlementAccount(
        bankName = view?.bankName ?: "",
        maskedAccountNumber = view?.accountNoMasked ?: "",
        holderName = view?.holderName ?: ""
    )
}


