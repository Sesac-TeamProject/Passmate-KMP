package org.sesacteamproject.passmate.core.di

import org.koin.dsl.module
import org.sesacteamproject.passmate.core.network.ApiClient
import org.sesacteamproject.passmate.core.network.SessionEventStream
import org.sesacteamproject.passmate.core.network.StompClient
import org.sesacteamproject.passmate.core.network.defaultApiBaseUrl
import org.sesacteamproject.passmate.core.network.defaultWsUrl

val coreModule = module {
    single { ApiClient(get(), defaultApiBaseUrl()) }
    single { StompClient(get(), defaultWsUrl()) }
    single { SessionEventStream(get()) }
}
