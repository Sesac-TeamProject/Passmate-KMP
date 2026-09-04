package org.sesacteamproject.passmate.auth.data.dto

import kotlinx.serialization.Serializable

// POST /auth/dev-login 응답 — 서버 `LoginResponse`와 1:1.
// user·expiresIn은 앱이 쓰지 않아 받지 않는다 (ApiClient가 ignoreUnknownKeys)
@Serializable
data class DevLoginResponse(
    val accessToken: String = "",
    val refreshToken: String = "",
    val isNewUser: Boolean = false
)
