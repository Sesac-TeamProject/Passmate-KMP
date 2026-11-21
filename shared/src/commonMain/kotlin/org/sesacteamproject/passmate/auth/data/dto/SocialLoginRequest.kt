package org.sesacteamproject.passmate.auth.data.dto

import kotlinx.serialization.Serializable

// POST /auth/login/{provider} 요청 — 서버 `SocialLoginRequest`와 1:1.
// 구글: 서버는 idToken과 authorizationCode가 함께 오면 400으로 거절한다 — 앱은 idToken 하나만 싣는다.
// 애플: idToken으로 로그인하고, authorizationCode는 서버가 refresh token으로 바꿔 두었다가 탈퇴 때
// Apple 연결을 끊는 데 쓴다(App Store 5.1.1(v)). name은 Apple이 첫 인증 때만 주는 이름이다.
// null 필드는 실려 나가지 않는다(ApiClient explicitNulls=false)
@Serializable
data class SocialLoginRequest(
    val idToken: String,
    val authorizationCode: String? = null,
    val name: String? = null
)
