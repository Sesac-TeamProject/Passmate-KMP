package org.sesacteamproject.passmate.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
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

    // 기본값은 플랫폼마다 다르다 — 안드로이드 에뮬레이터만 호스트 PC를 10.0.2.2로 본다
    @Test
    fun fallsBackToPlatformDefaultWhenGiven() {
        val emulatorHost = "10.0.2.2:8080"

        assertEquals(emulatorHost, resolveServerHost(null, emulatorHost))
        assertEquals(emulatorHost, resolveServerHost("", emulatorHost))
        assertEquals(emulatorHost, resolveServerHost("\$(PASSMATE_SERVER_HOST)", emulatorHost))
        assertEquals("172.31.98.123:8080", resolveServerHost("172.31.98.123:8080", emulatorHost))
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
}
