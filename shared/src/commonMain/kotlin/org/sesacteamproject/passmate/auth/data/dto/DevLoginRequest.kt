package org.sesacteamproject.passmate.auth.data.dto

import kotlinx.serialization.Serializable

// POST /auth/dev-login 요청 — 서버 `DevLoginRequest`와 1:1 (nickname·email은 선택이라 보내지 않는다)
@Serializable
data class DevLoginRequest(
    val key: String
)
