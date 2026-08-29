package org.sesacteamproject.passmate.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.ui.auth.SignInViewModel
import org.sesacteamproject.passmate.ui.home.RoomListViewModel
import org.sesacteamproject.passmate.ui.hostroom.CreateRoomViewModel
import org.sesacteamproject.passmate.ui.hostroom.HostedRoomsViewModel
import org.sesacteamproject.passmate.ui.join.JoinViewModel
import org.sesacteamproject.passmate.ui.mypage.MyInfoViewModel
import org.sesacteamproject.passmate.ui.payment.CoinHistoryViewModel
import org.sesacteamproject.passmate.ui.payment.PaymentViewModel
import org.sesacteamproject.passmate.ui.mypage.ReputationViewModel
import org.sesacteamproject.passmate.ui.play.PlayViewModel
import org.sesacteamproject.passmate.ui.profile.HostProfileViewModel
import org.sesacteamproject.passmate.ui.result.ResultViewModel
import org.sesacteamproject.passmate.ui.waiting.WaitingViewModel

val viewModelModule = module {
    factory { SignInViewModel(get(), get(), get()) }
    factory { RoomListViewModel(get()) }
    factory { JoinViewModel(get(), get(), get(), get()) }
    factory { PaymentViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { CoinHistoryViewModel(get()) }
    factory { WaitingViewModel(get(), get(), get(), get(), get()) }
    factory { PlayViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { ResultViewModel(get(), get(), get(), get(), get(), get(), get()) }
    factory { MyInfoViewModel(get(), get()) }
    factory { ReputationViewModel(get(), get(), get()) }
    factory { HostProfileViewModel(get(), get(), get(), get()) }
    factory { HostedRoomsViewModel(get(), get(), get()) }
    factory { CreateRoomViewModel(get(), get()) }
}
