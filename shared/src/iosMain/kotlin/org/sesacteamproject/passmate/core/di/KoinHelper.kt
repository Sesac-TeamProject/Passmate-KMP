package org.sesacteamproject.passmate.core.di

import org.koin.mp.KoinPlatform
import org.sesacteamproject.passmate.auth.domain.usecase.BuildGoogleSignInUrlUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.CompleteSignInUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.IsSignedInUseCase
import org.sesacteamproject.passmate.core.network.SessionEventStreamWatcher
import org.sesacteamproject.passmate.room.domain.policy.JoinInputPolicy
import org.sesacteamproject.passmate.room.domain.usecase.GetMyParticipationUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetParticipantsUseCase
import org.sesacteamproject.passmate.room.domain.usecase.GetRoomInfoUseCase
import org.sesacteamproject.passmate.room.domain.usecase.JoinRoomUseCase
import org.sesacteamproject.passmate.room.domain.usecase.LeaveRoomUseCase

// Swift는 reified 제네릭을 못 쓰므로 화면(Swift VM)별 의존성을 명시 getter로 노출한다 (아키텍처 설계 §4-5)
object KoinHelper {

    fun doInitKoin() {
        initKoin()
    }

    fun buildGoogleSignInUrlUseCase(): BuildGoogleSignInUrlUseCase = KoinPlatform.getKoin().get()

    fun completeSignInUseCase(): CompleteSignInUseCase = KoinPlatform.getKoin().get()

    fun isSignedInUseCase(): IsSignedInUseCase = KoinPlatform.getKoin().get()

    fun joinInputPolicy(): JoinInputPolicy = KoinPlatform.getKoin().get()

    fun getRoomInfoUseCase(): GetRoomInfoUseCase = KoinPlatform.getKoin().get()

    fun joinRoomUseCase(): JoinRoomUseCase = KoinPlatform.getKoin().get()

    fun getParticipantsUseCase(): GetParticipantsUseCase = KoinPlatform.getKoin().get()

    fun leaveRoomUseCase(): LeaveRoomUseCase = KoinPlatform.getKoin().get()

    fun getMyParticipationUseCase(): GetMyParticipationUseCase = KoinPlatform.getKoin().get()

    fun sessionEventStreamWatcher(): SessionEventStreamWatcher = SessionEventStreamWatcher(KoinPlatform.getKoin().get())
}
