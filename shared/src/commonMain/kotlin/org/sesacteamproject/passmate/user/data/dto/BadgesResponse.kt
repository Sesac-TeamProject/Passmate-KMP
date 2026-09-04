package org.sesacteamproject.passmate.user.data.dto

import kotlinx.serialization.Serializable

// GET /users/me/badges 응답 — 계약 `BadgeCollectionResponse`와 1:1
@Serializable
data class BadgesResponse(
    val achievedCount: Int = 0,
    val totalCount: Int = 0,
    val badges: List<BadgeDto> = emptyList()
) {

    @Serializable
    data class BadgeDto(
        val code: String = "",
        val name: String = "",
        val description: String? = null,
        val iconUrl: String? = null,
        val achieved: Boolean = false,
        val achievedAt: String? = null,
        val progress: Int? = null,
        // 서버는 target만 double로 준다 (OpenAPI BadgeResponse: progress int32 / target double)
        val target: Double? = null
    )
}
