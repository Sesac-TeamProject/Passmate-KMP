package org.sesacteamproject.passmate.core.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.sesacteamproject.passmate.core.storage.TokenStorage

// Context는 앱 시작점의 initKoin { androidContext(...) } 등록에 의존한다
actual val platformCoreModule: Module = module {
    single { TokenStorage(get()) }
}
