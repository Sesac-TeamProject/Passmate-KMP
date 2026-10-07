package org.sesacteamproject.passmate.auth.data.dto

import kotlinx.serialization.Serializable

// POST /auth/login/{provider} 요청 — 서버 `SocialLoginRequest`와 1:1.
// 서버는 idToken과 authorizationCode가 함께 오면 400으로 거절한다. 앱은 네이티브 SDK가 준
// ID 토큰만 보내므로 idToken 하나만 싣는다 (authorizationCode는 웹 리다이렉트 플로우용이다)
// name은 Apple 전용 — Apple은 이름을 토큰에 싣지 않아 첫 인증 때 받은 값을 보낸다(null이면 생략된다)
@Serializable
data class SocialLoginRequest(
    val idToken: String,
    val name: String? = null
)
