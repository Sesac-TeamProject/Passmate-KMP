package org.sesacteamproject.passmate.di

import kotlin.test.Test
import kotlin.test.assertNotNull
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import org.sesacteamproject.passmate.core.di.initKoin
import org.sesacteamproject.passmate.navigation.AppShellViewModel
import org.sesacteamproject.passmate.ui.auth.SignInViewModel
import org.sesacteamproject.passmate.ui.join.JoinViewModel
import org.sesacteamproject.passmate.ui.mypage.JoinedRoomsViewModel
import org.sesacteamproject.passmate.ui.mypage.MyInfoViewModel
import org.sesacteamproject.passmate.ui.play.PlayViewModel
import org.sesacteamproject.passmate.ui.result.ResultViewModel
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
        val playViewModel = KoinPlatform.getKoin().get<PlayViewModel>()
        val resultViewModel = KoinPlatform.getKoin().get<ResultViewModel>()
        val myInfoViewModel = KoinPlatform.getKoin().get<MyInfoViewModel>()
        val joinedRoomsViewModel = KoinPlatform.getKoin().get<JoinedRoomsViewModel>()
        val appShellViewModel = KoinPlatform.getKoin().get<AppShellViewModel>()

        assertNotNull(signInViewModel)
        assertNotNull(joinViewModel)
        assertNotNull(waitingViewModel)
        assertNotNull(playViewModel)
        assertNotNull(resultViewModel)
        assertNotNull(myInfoViewModel)
        assertNotNull(joinedRoomsViewModel)
        assertNotNull(appShellViewModel)
        stopKoin()
    }
}
