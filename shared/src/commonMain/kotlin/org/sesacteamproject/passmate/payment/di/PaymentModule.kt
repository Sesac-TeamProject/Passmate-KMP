package org.sesacteamproject.passmate.payment.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.payment.data.remote.PaymentRemoteDataSource
import org.sesacteamproject.passmate.payment.data.repository.PaymentRepositoryImpl
import org.sesacteamproject.passmate.payment.domain.policy.CoinPolicy
import org.sesacteamproject.passmate.payment.domain.repository.PaymentRepository
import org.sesacteamproject.passmate.payment.domain.usecase.ConfirmChargeUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetCoinTransactionsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetEarningsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetMyCoinsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetPublicRoomsUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.GetSettlementAccountUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.SaveSettlementAccountUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.SetPaymentMethodUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.PayEntryFeeUseCase
import org.sesacteamproject.passmate.payment.domain.usecase.RequestChargeUseCase

val paymentModule = module {
    single { PaymentRemoteDataSource(get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get()) }
    factory { CoinPolicy() }
    factory { GetMyCoinsUseCase(get()) }
    factory { GetCoinTransactionsUseCase(get()) }
    factory { RequestChargeUseCase(get()) }
    factory { ConfirmChargeUseCase(get()) }
    factory { PayEntryFeeUseCase(get()) }
    factory { GetPublicRoomsUseCase(get()) }
    factory { GetEarningsUseCase(get()) }
    factory { GetSettlementAccountUseCase(get()) }
    factory { SaveSettlementAccountUseCase(get()) }
    factory { SetPaymentMethodUseCase(get()) }
}
