package org.sesacteamproject.passmate.core.network

// Desktop은 맥에서 도는 개발 미러라 기본값이 로컬 백엔드다. 안드로이드와 같은
// local.properties의 PASSMATE_SERVER_HOST로 덮어써 세 타깃이 한 서버를 본다
private val serverHost: String by lazy {
    resolveServerHost(BUILD_SERVER_HOST)
}

actual fun defaultApiBaseUrl(): String = apiBaseUrlOf(serverHost)

actual fun defaultWsUrl(): String = wsUrlOf(serverHost)
