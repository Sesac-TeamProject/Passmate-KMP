package org.sesacteamproject.passmate.core.network

// 기본값은 공통(운영 서버)을 그대로 쓴다. 로컬 백엔드를 볼 때만 local.properties(gitignore)의
// PASSMATE_SERVER_HOST를 적는다 — 에뮬레이터는 `10.0.2.2:8080`(호스트 PC 별칭),
// 실기기는 이 별칭에 닿지 못하므로 맥의 LAN 주소를 적는다
private val serverHost: String by lazy {
    resolveServerHost(BUILD_SERVER_HOST)
}

actual fun defaultApiBaseUrl(): String = apiBaseUrlOf(serverHost)

actual fun defaultWsUrl(): String = wsUrlOf(serverHost)
