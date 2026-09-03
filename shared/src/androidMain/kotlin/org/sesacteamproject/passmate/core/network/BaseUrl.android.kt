package org.sesacteamproject.passmate.core.network

// Android 에뮬레이터에서 호스트 PC의 로컬 백엔드 접근 주소
actual fun defaultApiBaseUrl(): String = "http://10.0.2.2:8080"

actual fun defaultWsUrl(): String = "ws://10.0.2.2:8080/ws"
