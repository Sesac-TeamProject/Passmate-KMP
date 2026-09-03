package org.sesacteamproject.passmate.room.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.room.data.remote.RoomRemoteDataSource
import org.sesacteamproject.passmate.room.data.repository.RoomRepositoryImpl
import org.sesacteamproject.passmate.room.domain.policy.JoinInputPolicy
import org.sesacteamproject.passmate.room.domain.repository.RoomRepository
import org.sesacteamproject.passmate.room.domain.usecase.CreateRoomUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetHostedRoomsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetMyParticipationUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetParticipantsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomPinUseCase
import org.sesacteamproject.passmate.room.domain.usecase.JoinRoomUseCase
import org.sesacteamproject.passmate.room.domain.usecase.LeaveRoomUseCase

val roomModule = module {
    single { RoomRemoteDataSource(get()) }
    single<RoomRepository> { RoomRepositoryImpl(get(), get()) }
    factory { JoinInputPolicy() }
    factory { GetRoomInfoUseCase(get()) }
    factory { GetRoomPinUseCase(get()) }
    factory { JoinRoomUseCase(get()) }
    factory { GetParticipantsUseCase(get()) }
    factory { LeaveRoomUseCase(get()) }
    factory { GetMyParticipationUseCase(get()) }
    factory { GetHostedRoomsUseCase(get()) }
    factory { CreateRoomUseCase(get()) }
}
