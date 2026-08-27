package org.sesacteamproject.passmate.auth.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.auth.data.repository.AuthRepositoryImpl
import org.sesacteamproject.passmate.auth.domain.repository.AuthRepository
import org.sesacteamproject.passmate.auth.domain.usecase.BuildGoogleSignInUrlUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.CompleteSignInUseCase

val authModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    factory { BuildGoogleSignInUrlUseCase(get()) }
    factory { CompleteSignInUseCase(get()) }
}
