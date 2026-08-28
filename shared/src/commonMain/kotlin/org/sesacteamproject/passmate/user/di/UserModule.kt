package org.sesacteamproject.passmate.user.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.user.data.remote.UserRemoteDataSource
import org.sesacteamproject.passmate.user.data.repository.UserRepositoryImpl
import org.sesacteamproject.passmate.user.domain.repository.UserRepository
import org.sesacteamproject.passmate.user.domain.usecase.GetMyPageUseCase

val userModule = module {
    single { UserRemoteDataSource(get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }
    factory { GetMyPageUseCase(get()) }
}
