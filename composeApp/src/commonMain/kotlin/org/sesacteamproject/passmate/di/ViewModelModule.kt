package org.sesacteamproject.passmate.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.ui.auth.SignInViewModel

val viewModelModule = module {
    factory { SignInViewModel(get(), get()) }
}
