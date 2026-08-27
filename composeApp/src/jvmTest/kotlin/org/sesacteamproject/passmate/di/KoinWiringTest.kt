package org.sesacteamproject.passmate.di

import kotlin.test.Test
import kotlin.test.assertNotNull
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.sesacteamproject.passmate.core.di.initKoin
import org.sesacteamproject.passmate.ui.auth.SignInViewModel

// 등록 누락·순환을 실행 전에 검출하는 배선 정합성 테스트 (아키텍처 설계 §6)
class KoinWiringTest {

    @Test
    fun resolveSignInViewModelFromModules() {
        initKoin {
            modules(viewModelModule)
        }

        val viewModel = KoinPlatform.getKoin().get<SignInViewModel>()

        assertNotNull(viewModel)
        stopKoin()
    }
}
