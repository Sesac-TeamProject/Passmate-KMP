package org.sesacteamproject.passmate.auth.data.dto

import kotlinx.serialization.Serializable

// POST /auth/dev-login 응답 — 서버 `LoginResponse`와 1:1.
// user·expiresIn은 앱이 쓰지 않아 받지 않는다 (ApiClient가 ignoreUnknownKeys)
// 토큰 두 개는 서버 필수 필드라 기본값을 두지 않는다 — 기본값을 두면 계약이 어긋났을 때
// 빈 토큰으로 로그인을 시도하고 "콜백 토큰이 비어 있습니다"라는 엉뚱한 문구가 뜬다
@Serializable
data class DevLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val isNewUser: Boolean = false
)
