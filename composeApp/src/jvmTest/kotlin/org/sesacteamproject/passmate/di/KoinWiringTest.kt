package org.sesacteamproject.passmate.di

import kotlin.test.Test
import kotlin.test.assertNotNull
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.sesacteamproject.passmate.core.di.initKoin
import org.sesacteamproject.passmate.ui.auth.SignInViewModel
import org.sesacteamproject.passmate.ui.join.JoinViewModel
import org.sesacteamproject.passmate.ui.waiting.WaitingViewModel

// 등록 누락·순환을 실행 전에 검출하는 배선 정합성 테스트 (아키텍처 설계 §6)
class KoinWiringTest {

    @Test
    fun resolveScreenViewModelsFromModules() {
        initKoin {
            modules(viewModelModule)
        }

        val signInViewModel = KoinPlatform.getKoin().get<SignInViewModel>()
        val joinViewModel = KoinPlatform.getKoin().get<JoinViewModel>()
        val waitingViewModel = KoinPlatform.getKoin().get<WaitingViewModel>()

        assertNotNull(signInViewModel)
        assertNotNull(joinViewModel)
        assertNotNull(waitingViewModel)
        stopKoin()
    }
}
