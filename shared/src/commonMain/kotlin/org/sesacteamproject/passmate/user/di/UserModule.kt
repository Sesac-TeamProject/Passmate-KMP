package org.sesacteamproject.passmate.user.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.user.data.remote.UserRemoteDataSource
import org.sesacteamproject.passmate.user.data.repository.UserRepositoryImpl
import org.sesacteamproject.passmate.user.domain.model.PendingGuestClaim
import org.sesacteamproject.passmate.user.domain.repository.UserRepository
import org.sesacteamproject.passmate.user.domain.usecase.BlockHostUseCase
import org.sesacteamproject.passmate.user.domain.usecase.CompleteGuestClaimUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetHostProfileUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetMyBadgesUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetMyGradeUseCase
import org.sesacteamproject.passmate.user.domain.usecase.GetMyPageUseCase
import org.sesacteamproject.passmate.user.domain.usecase.ReportHostUseCase
import org.sesacteamproject.passmate.user.domain.usecase.RequestGuestClaimUseCase

val userModule = module {
    single { UserRemoteDataSource(get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }
    single { PendingGuestClaim() }
    factory { GetMyPageUseCase(get()) }
    factory { RequestGuestClaimUseCase(get()) }
    factory { CompleteGuestClaimUseCase(get(), get()) }
    factory { GetMyGradeUseCase(get()) }
    factory { GetMyBadgesUseCase(get()) }
    factory { GetHostProfileUseCase(get()) }
    factory { BlockHostUseCase(get()) }
    factory { ReportHostUseCase(get()) }
}
