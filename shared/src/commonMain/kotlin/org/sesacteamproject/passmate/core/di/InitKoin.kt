package org.sesacteamproject.passmate.core.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.sesacteamproject.passmate.auth.di.authModule

// 플랫폼 시작점 3곳(PassmateApplication·jvm main·iOS KoinHelper)에서만 호출한다.
// 기능(스토리) 모듈은 구현되는 스토리 증분마다 여기에 추가한다 (아키텍처 설계 §4-2).
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            coreModule,
            platformCoreModule,
            authModule
        )
    }
}
