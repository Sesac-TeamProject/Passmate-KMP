package org.sesacteamproject.passmate.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.ui.auth.SignInViewModel
import org.sesacteamproject.passmate.ui.join.JoinViewModel
import org.sesacteamproject.passmate.ui.play.PlayViewModel
import org.sesacteamproject.passmate.ui.waiting.WaitingViewModel

val viewModelModule = module {
    factory { SignInViewModel(get(), get()) }
    factory { JoinViewModel(get(), get(), get(), get()) }
    factory { WaitingViewModel(get(), get(), get(), get(), get()) }
    factory { PlayViewModel(get(), get(), get(), get(), get(), get(), get()) }
}
