package org.sesacteamproject.passmate.auth.data.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

// POST /auth/login/{provider} — 서버 `SocialLoginRequest`·`LoginResponse`와 1:1.
// dev-login도 같은 `LoginResponse`를 돌려주므로 응답 DTO는 하나를 공유한다.
@OptIn(ExperimentalSerializationApi::class)
class SocialLoginTest {

    // ApiClient와 같은 설정 — explicitNulls=false라 null 필드는 실려 나가지 않는다
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun parsesServerLoginResponseAndIgnoresUserBlock() {
        val raw = """
            {
              "isNewUser": true,
              "accessToken": "access-jwt",
              "refreshToken": "refresh-jwt",
              "expiresIn": 1800,
              "user": {
                "id": 7,
                "nickname": "패스메이트",
                "email": "member@example.test",
                "profileImageUrl": null,
                "defaultAvatarId": "avatar_1",
                "isAdmin": false
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<LoginResponse>(raw)

        assertEquals("access-jwt", response.accessToken)
        assertEquals("refresh-jwt", response.refreshToken)
        assertTrue(response.isNewUser)
    }

    @Test
    fun parsesDevLoginResponseWithoutIsNewUser() {
        val raw = """
            {
              "accessToken": "access-jwt",
              "refreshToken": "refresh-jwt",
              "expiresIn": 1800
            }
        """.trimIndent()

        val response = json.decodeFromString<LoginResponse>(raw)

        assertEquals("access-jwt", response.accessToken)
        assertFalse(response.isNewUser)
    }

    // 서버는 idToken과 authorizationCode가 함께 오면 400으로 거절한다 — 하나만 실려야 한다
    @Test
    fun sendsIdTokenOnly() {
        val body = json.encodeToString(SocialLoginRequest.serializer(), SocialLoginRequest(idToken = "google-id-token"))

        assertEquals("""{"idToken":"google-id-token"}""", body)
    }

    // Apple은 인가 코드를 함께 싣고(탈퇴 시 연결 해제용), 이름은 첫 인증 때만 준다 — 받았을 때만 name을 싣는다
    @Test
    fun sendsAppleCodeAndNameOnlyWhenPresent() {
        val first = json.encodeToString(
            SocialLoginRequest.serializer(),
            SocialLoginRequest(idToken = "apple-id-token", authorizationCode = "apple-code", name = "홍희표")
        )
        val later = json.encodeToString(
            SocialLoginRequest.serializer(),
            SocialLoginRequest(idToken = "apple-id-token", authorizationCode = "apple-code")
        )

        assertEquals("""{"idToken":"apple-id-token","authorizationCode":"apple-code","name":"홍희표"}""", first)
        assertEquals("""{"idToken":"apple-id-token","authorizationCode":"apple-code"}""", later)
    }
}
