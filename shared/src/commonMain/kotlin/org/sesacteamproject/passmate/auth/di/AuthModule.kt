package org.sesacteamproject.passmate.auth.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.auth.data.remote.AuthRemoteDataSource
import org.sesacteamproject.passmate.auth.data.repository.AuthRepositoryImpl
import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository
import org.sesacteamproject.passmate.auth.domain.usecase.BuildGoogleSignInUrlUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.CompleteSignInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.SignOutUseCase

val authModule = module {
    single { AuthRemoteDataSource(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    factory { BuildGoogleSignInUrlUseCase(get()) }
    factory { CompleteSignInUseCase(get()) }
    factory { IsSignedInUseCase(get()) }
    factory { SignOutUseCase(get()) }
}
