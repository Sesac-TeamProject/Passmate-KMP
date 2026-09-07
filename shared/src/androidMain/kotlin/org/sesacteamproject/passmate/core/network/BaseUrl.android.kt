package org.sesacteamproject.passmate.core.network

// Android 에뮬레이터에서 호스트 PC의 로컬 백엔드 접근 주소
private const val EMULATOR_SERVER_HOST = "10.0.2.2:8080"

actual fun defaultApiBaseUrl(): String = apiBaseUrlOf(EMULATOR_SERVER_HOST)

actual fun defaultWsUrl(): String = wsUrlOf(EMULATOR_SERVER_HOST)
