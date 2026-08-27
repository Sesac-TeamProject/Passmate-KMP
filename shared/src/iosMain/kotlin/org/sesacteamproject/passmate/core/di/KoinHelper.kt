package org.sesacteamproject.passmate.core.di

import org.koin.mp.KoinPlatform
import org.sesacteamproject.passmate.auth.domain.usecase.BuildGoogleSignInUrlUseCase
import org.sesacteamproject.passmate.auth.domain.usecase.CompleteSignInUseCase

// Swift는 reified 제네릭을 못 쓰므로 화면(Swift VM)별 의존성을 명시 getter로 노출한다 (아키텍처 설계 §4-5)
object KoinHelper {

    fun doInitKoin() {
        initKoin()
    }

    fun buildGoogleSignInUrlUseCase(): BuildGoogleSignInUrlUseCase = KoinPlatform.getKoin().get()

    fun completeSignInUseCase(): CompleteSignInUseCase = KoinPlatform.getKoin().get()
}
