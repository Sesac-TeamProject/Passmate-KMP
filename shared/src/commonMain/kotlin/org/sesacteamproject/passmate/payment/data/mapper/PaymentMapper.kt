package org.sesacteamproject.passmate.payment.data.mapper

import org.sesacteamproject.passmate.core.model.PagedResult
import org.sesacteamproject.passmate.payment.data.dto.ChargeCheckoutResponse
import org.sesacteamproject.passmate.payment.data.dto.CoinBalanceResponse
import org.sesacteamproject.passmate.payment.data.dto.EarningsResponse
import org.sesacteamproject.passmate.payment.data.dto.SettlementAccountDto
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
        roomId = roomId,
        pin = pin,
        title = title,
        topic = topic,
        hostId = hostId,
        hostName = hostName,
        hostLevel = hostLevel,
        hostRating = hostRating,
        status = RoomStatus.from(status),
        participantCount = participantCount,
        maxParticipants = maxParticipants,
        isPaid = isPaid,
        entryFee = entryFee,
        scheduledAt = scheduledAt
    )
}

fun PublicRoomPageResponse.toDomain(): PagedResult<PublicRoom> {
    return PagedResult(
        items = items.map { it.toDomain() },
        nextCursor = nextCursor,
        hasNext = hasNext
    )
}

fun EarningsResponse.toDomain(): Earnings {
    return Earnings(
        monthlyTotal = monthlyTotal,
        hostSharePercent = hostSharePercent,
        nextPayout = nextPayout?.let { NextPayout(dateLabel = it.dateLabel, amount = it.amount) },
        paidRoomCount = paidRoomCount,
        studentCount = studentCount,
        items = items.map { it.toDomain() },
        nextCursor = nextCursor,
        hasNext = hasNext,
        account = account?.let {
            SettlementAccountSummary(
                bankName = it.bankName,
                maskedNumber = it.maskedNumber,
                payoutNote = it.payoutNote
            )
        }
    )
}

fun EarningsResponse.SettlementItemDto.toDomain(): SettlementItem {
    return SettlementItem(
        settlementId = settlementId,
        dateLabel = dateLabel,
        roomTitle = roomTitle,
        participantCount = participantCount,
        entryFeeTotal = entryFeeTotal,
        feeAmount = feeAmount,
        payoutAmount = payoutAmount,
        status = SettlementStatus.from(status)
    )
}

fun SettlementAccountDto.toDomain(): SettlementAccount {
    return SettlementAccount(
        bankName = bankName,
        accountNumber = accountNumber,
        holderName = holderName
    )
}
