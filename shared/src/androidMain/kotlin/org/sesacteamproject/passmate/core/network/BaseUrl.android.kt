package org.sesacteamproject.passmate.core.network

// 에뮬레이터가 호스트 PC를 보는 주소. 실기기는 이 별칭에 닿지 못하므로
// local.properties(gitignore)의 PASSMATE_SERVER_HOST에 맥의 LAN 주소를 적어 덮어쓴다
private const val EMULATOR_SERVER_HOST = "10.0.2.2:8080"

private val serverHost: String by lazy {
    resolveServerHost(BUILD_SERVER_HOST, EMULATOR_SERVER_HOST)
}

actual fun defaultApiBaseUrl(): String = apiBaseUrlOf(serverHost)

actual fun defaultWsUrl(): String = wsUrlOf(serverHost)
