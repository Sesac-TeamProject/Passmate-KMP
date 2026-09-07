package org.sesacteamproject.passmate.core.network

// 로컬 백엔드 기준 기본값 — 배포 환경 전환은 추후 빌드 설정으로 분리한다
expect fun defaultApiBaseUrl(): String

expect fun defaultWsUrl(): String

// 시뮬레이터/Desktop 기본값. 실기기는 빌드 설정으로 맥의 LAN 주소를 덮어쓴다
internal const val DEFAULT_SERVER_HOST = "localhost:8080"

// 스킴은 코드에서 붙인다 — xcconfig는 `//`를 주석으로 읽어 전체 URL을 값으로 둘 수 없다
internal fun apiBaseUrlOf(host: String): String = "http://$host"

internal fun wsUrlOf(host: String): String = "ws://$host/ws"

// 빌드 설정이 비어 있으면 Info.plist의 `$(...)`가 치환되지 않은 채 그대로 들어온다.
// 기본값은 플랫폼이 준다 — 안드로이드 에뮬레이터만 호스트 PC를 10.0.2.2로 본다
internal fun resolveServerHost(configured: String?, default: String = DEFAULT_SERVER_HOST): String {
    val host = configured?.trim().orEmpty()

    return if (host.isEmpty() || host.startsWith("\$(")) {
        default
    } else {
        host
    }
}
