package org.sesacteamproject.passmate.user.data.dto

import kotlinx.serialization.Serializable

// GET /users/me/badges 응답 — contracts §평가·등급과 1:1
@Serializable
data class BadgesResponse(
    val items: List<BadgeDto> = emptyList()
) {

    @Serializable
    data class BadgeDto(
        val type: String = "",
        val earned: Boolean = false,
        val earnedAt: String? = null,
        val progressCurrent: Int? = null,
        val progressTarget: Int? = null
    )
}
