package org.sesacteamproject.passmate.core.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.sesacteamproject.passmate.core.storage.TokenStorage

actual val platformCoreModule: Module = module {
    single { TokenStorage() }
}
