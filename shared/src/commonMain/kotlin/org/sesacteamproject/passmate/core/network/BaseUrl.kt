package org.sesacteamproject.passmate.core.network

// 운영 서버 기준 기본값 — 로컬 백엔드로 붙을 때만 설정 파일에서 덮어쓴다
expect fun defaultApiBaseUrl(): String

expect fun defaultWsUrl(): String

// 기본 접속 대상은 운영 서버다(https://api.passmate.kr · wss://api.passmate.kr/ws). 포트를
// 적지 않아 TLS 기본 포트(443)로 간다. apex 도메인 passmate.kr은 웹 프론트엔드(Next.js)라
// API·STOMP가 없다 — 호스트는 반드시 api 서브도메인이어야 한다.
// 로컬 백엔드를 볼 때만 gitignore된 설정 한 줄로 덮어쓴다 — Android·Desktop은 local.properties,
// iOS는 Local.xcconfig의 PASSMATE_SERVER_HOST다 (에뮬레이터는 호스트 PC를 10.0.2.2로 보므로
// `10.0.2.2:8080`, 실기기는 이 별칭에 닿지 못하므로 맥의 LAN 주소를 적는다)
internal const val DEFAULT_SERVER_HOST = "api.passmate.kr"

// 스킴은 코드에서 붙인다 — xcconfig는 `//`를 주석으로 읽어 전체 URL을 값으로 둘 수 없다.
// 로컬·사설망은 평문(http/ws), 그 밖(운영 도메인)은 TLS(https/wss)로 간다. 설정에는 호스트만
// 두고 스킴을 여기서 정하므로, 서버를 바꿀 때 고치는 곳은 여전히 gitignore된 설정 파일 한 줄이다
internal fun apiBaseUrlOf(host: String): String =
    if (isLocalDevHost(host)) "http://$host" else "https://$host"

internal fun wsUrlOf(host: String): String =
    if (isLocalDevHost(host)) "ws://$host/ws" else "wss://$host/ws"

// 빌드 설정이 비어 있으면 Info.plist의 `$(...)`가 치환되지 않은 채 그대로 들어온다.
// 그 경우와 값이 없는 경우 모두 운영 서버(DEFAULT_SERVER_HOST)로 떨어진다
internal fun resolveServerHost(configured: String?): String {
    val host = configured?.trim().orEmpty()

    return if (host.isEmpty() || host.startsWith("\$(")) {
        DEFAULT_SERVER_HOST
    } else {
        host
    }
}
