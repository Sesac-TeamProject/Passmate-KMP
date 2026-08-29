package org.sesacteamproject.passmate.user.data.dto

import kotlinx.serialization.Serializable

// GET /users/me 응답 — contracts §Users와 1:1
@Serializable
data class UserProfileResponse(
    val nickname: String = "",
    val email: String? = null,
    val joinedAt: String? = null,
    val avatarId: Int? = null,
    val level: Int? = null,
    val coins: Long? = null,
    val joinedRoomCount: Int? = null,
    val hostedRoomCount: Int? = null
)

// PUT /users/me 요청 — 닉네임·기본 캐릭터 수정 (M-12-1·M-12-7)
@Serializable
data class UpdateProfileRequest(
    val nickname: String? = null,
    val avatarId: Int? = null
)

// GET/PUT /users/me/notification-settings — contracts §Users와 1:1
@Serializable
data class NotificationSettingsDto(
    val sessionStart: Boolean = true,
    val ratingRequest: Boolean = true,
    val settlementDone: Boolean = true
)
