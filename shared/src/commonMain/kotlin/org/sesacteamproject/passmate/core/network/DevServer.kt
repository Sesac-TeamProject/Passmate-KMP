package org.sesacteamproject.passmate.core.network

// 개발용 로그인(POST /auth/dev-login)은 로컬 백엔드에만 있고 운영에는 배포되지 않는다.
// 그래서 노출 조건을 빌드 타입이 아니라 "지금 붙어 있는 서버가 개발 서버인가"로 둔다 —
// 엔드포인트가 존재하는 전제와 1:1이라 운영 URL을 보는 순간 자동으로 사라진다.
private val LOCAL_HOSTS = setOf("localhost", "127.0.0.1", "::1", "10.0.2.2")

private fun String.isPrivateNetworkHost(): Boolean {
    val parts = split(".")

    return if (parts.size != 4 || parts.any { it.toIntOrNull() == null }) {
        false
    } else {
        val first = parts[0].toInt()
        val second = parts[1].toInt()

        first == 10 || (first == 192 && second == 168) || (first == 172 && second in 16..31)
    }
}

// "http://10.0.2.2:8080" → "10.0.2.2", "http://[::1]:8080" → "::1"
internal fun hostOf(baseUrl: String): String {
    val withoutScheme = baseUrl.substringAfter("://")
    val withoutPath = withoutScheme.substringBefore("/")

    return if (withoutPath.startsWith("[")) {
        // IPv6는 주소 안에 콜론이 있어 포트부터 자르면 주소가 잘린다 — 대괄호를 먼저 판다
        withoutPath.substringAfter("[").substringBefore("]")
    } else {
        withoutPath.substringBefore(":")
    }
}

fun isLocalDevServer(baseUrl: String): Boolean {
    val host = hostOf(baseUrl)

    return host in LOCAL_HOSTS || host.isPrivateNetworkHost()
}
