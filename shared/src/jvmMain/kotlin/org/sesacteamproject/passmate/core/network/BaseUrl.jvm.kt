package org.sesacteamproject.passmate.core.network

actual fun defaultApiBaseUrl(): String = apiBaseUrlOf(DEFAULT_SERVER_HOST)

actual fun defaultWsUrl(): String = wsUrlOf(DEFAULT_SERVER_HOST)
