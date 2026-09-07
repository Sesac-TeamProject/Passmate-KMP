package org.sesacteamproject.passmate.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// 공개 엔드포인트(호스트 공개 프로필)는 게스트 토큰을 실으면 서버가 403 GUEST_NOT_ALLOWED로 막는다.
// 백엔드 CurrentUserArgumentResolver가 `required = false`인데도 게스트면 예외를 던지기 때문이다
// (2026-09-07 확인 — 토큰 없이 부르면 200). 회원 토큰은 차단 필터가 동작하도록 그대로 싣는다.
class AuthTokenSelectionTest {

    @Test
    fun sendsMemberTokenOnEveryRequest() {
        assertEquals("member", authTokenFor(isPublicEndpoint = false, accessToken = "member", guestToken = "guest"))
        assertEquals("member", authTokenFor(isPublicEndpoint = true, accessToken = "member", guestToken = "guest"))
    }

    @Test
    fun sendsGuestTokenOnlyToProtectedEndpoints() {
        assertEquals("guest", authTokenFor(isPublicEndpoint = false, accessToken = null, guestToken = "guest"))
        assertNull(authTokenFor(isPublicEndpoint = true, accessToken = null, guestToken = "guest"))
    }

    @Test
    fun sendsNothingWhenSignedOut() {
        assertNull(authTokenFor(isPublicEndpoint = false, accessToken = null, guestToken = null))
        assertNull(authTokenFor(isPublicEndpoint = true, accessToken = null, guestToken = null))
    }
}
