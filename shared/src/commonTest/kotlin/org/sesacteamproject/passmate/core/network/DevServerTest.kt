package org.sesacteamproject.passmate.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// 개발용 로그인 노출 조건 — 운영 URL에서 새면 안 된다
class DevServerTest {

    @Test
    fun treatsLocalAndPrivateHostsAsDevServer() {
        val devUrls = listOf(
            "http://localhost:8080",
            "http://127.0.0.1:8080",
            "http://10.0.2.2:8080",          // 안드로이드 에뮬레이터 별칭
            "http://172.16.0.182:8080",      // 맥 LAN IP
            "http://192.168.0.5:8080",
            "http://10.1.2.3:8080"
        )

        devUrls.forEach { url ->
            assertTrue(isLocalDevServer(url), "개발 서버로 봐야 한다: $url")
        }
    }

    @Test
    fun treatsPublicHostsAsProduction() {
        val prodUrls = listOf(
            "https://api.passmate.app",
            "https://passmate-backend.onrender.com",
            "http://13.125.1.2:8080",        // 공인 IP
            "https://172.32.0.1",            // 사설 대역(172.16~31) 밖
            "https://192.169.0.1"
        )

        prodUrls.forEach { url ->
            assertFalse(isLocalDevServer(url), "운영 서버로 봐야 한다: $url")
        }
    }

    @Test
    fun parsesHostWithoutSchemeAndPort() {
        assertEquals("172.16.0.182", hostOf("http://172.16.0.182:8080"))
        assertEquals("api.passmate.app", hostOf("https://api.passmate.app"))
        assertEquals("localhost", hostOf("http://localhost:8080/"))
    }
}
