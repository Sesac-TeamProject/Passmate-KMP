package org.sesacteamproject.passmate.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// 서버 주소를 빌드 설정에서 받는 경로 — 기기 테스트가 추적 파일을 건드리지 않게 하는 장치
class ServerHostTest {

    @Test
    fun buildsApiAndWsUrlFromHost() {
        val host = "192.168.45.4:8080"

        assertEquals("http://192.168.45.4:8080", apiBaseUrlOf(host))
        assertEquals("ws://192.168.45.4:8080/ws", wsUrlOf(host))
    }

    @Test
    fun fallsBackToDefaultWhenHostIsMissing() {
        assertEquals(DEFAULT_SERVER_HOST, resolveServerHost(null))
        assertEquals(DEFAULT_SERVER_HOST, resolveServerHost(""))
        assertEquals(DEFAULT_SERVER_HOST, resolveServerHost("   "))
    }

    // xcconfig에 값이 없으면 Info.plist의 $(...)가 치환되지 않은 채 그대로 들어온다
    @Test
    fun fallsBackWhenBuildSettingWasNotSubstituted() {
        assertEquals(DEFAULT_SERVER_HOST, resolveServerHost("\$(PASSMATE_SERVER_HOST)"))
    }

    // 설정이 있으면 세 타깃 모두 그 값을 쓴다 — 로컬 백엔드로 되돌리는 경로가 살아 있어야 한다
    @Test
    fun prefersConfiguredHostOverDefault() {
        assertEquals("10.0.2.2:8080", resolveServerHost("10.0.2.2:8080"))
        assertEquals("172.31.98.123:8080", resolveServerHost("172.31.98.123:8080"))
    }

    // 설정이 없으면 운영 서버로 간다 — 기본 접속 대상이 로컬 백엔드가 아니다
    @Test
    fun defaultsToProductionServer() {
        val defaultHost = resolveServerHost(null)

        assertEquals("api.passmate.kr", defaultHost)
        assertEquals("https://api.passmate.kr", apiBaseUrlOf(defaultHost))
        assertEquals("wss://api.passmate.kr/ws", wsUrlOf(defaultHost))
        assertFalse(isLocalDevServer(apiBaseUrlOf(defaultHost)), "기본 주소는 개발 서버가 아니다")
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals("192.168.45.4:8080", resolveServerHost("  192.168.45.4:8080  "))
    }

    // LAN IP로 바꿔도 dev-login이 계속 열려야 한다 — 기기 테스트가 막히면 의미가 없다
    @Test
    fun keepsDevLoginAvailableForLanHost() {
        val lanUrl = apiBaseUrlOf(resolveServerHost("192.168.45.4:8080"))

        assertTrue(isLocalDevServer(lanUrl), "LAN 주소는 개발 서버로 봐야 한다: $lanUrl")
    }

    // 운영 도메인은 TLS로 가야 한다 — 평문으로 붙으면 iOS ATS·안드로이드 평문 정책에 막힌다
    @Test
    fun usesTlsForRemoteHost() {
        val host = "api.passmate.kr"

        assertEquals("https://api.passmate.kr", apiBaseUrlOf(host))
        assertEquals("wss://api.passmate.kr/ws", wsUrlOf(host))
    }

    // 로컬·에뮬레이터 별칭은 인증서가 없으므로 평문을 유지한다
    @Test
    fun keepsCleartextForLocalHosts() {
        assertEquals("http://localhost:8080", apiBaseUrlOf("localhost:8080"))
        assertEquals("ws://10.0.2.2:8080/ws", wsUrlOf("10.0.2.2:8080"))
        assertEquals("http://[::1]:8080", apiBaseUrlOf("[::1]:8080"))
    }

    // 운영 서버에는 dev-login이 배포되지 않는다 — 버튼도 함께 사라져야 한다
    @Test
    fun hidesDevLoginForRemoteHost() {
        val remoteUrl = apiBaseUrlOf(resolveServerHost("api.passmate.kr"))

        assertEquals("https://api.passmate.kr", remoteUrl)
        assertFalse(isLocalDevServer(remoteUrl), "운영 주소는 개발 서버가 아니다: $remoteUrl")
    }
}
