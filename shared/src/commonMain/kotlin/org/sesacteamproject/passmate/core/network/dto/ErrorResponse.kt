package org.sesacteamproject.passmate.core.network.dto

import kotlinx.serialization.Serializable

// contracts/rest-api.md §공통 오류 코드 — 모든 오류 응답은 { code, message }
@Serializable
data class ErrorResponse(
    val code: String? = null,
    val message: String? = null
)
