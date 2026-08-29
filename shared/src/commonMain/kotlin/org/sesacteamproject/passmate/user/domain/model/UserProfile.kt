package org.sesacteamproject.passmate.user.domain.model

import org.sesacteamproject.passmate.room.domain.model.HostLevel

// 내 프로필+요약 (GET /users/me) — 설정 허브(M-12 내 정보 관리) 렌더링용
data class UserProfile(
    val nickname: String,
    val email: String?,
    val joinedAt: String?,
    val avatarId: Int?,
    val level: HostLevel?,
    val coins: Long?,
    val joinedRoomCount: Int?,
    val hostedRoomCount: Int?
)

// 알림 설정 3종 (GET/PUT /users/me/notification-settings) — M-12-10
data class NotificationSettings(
    val sessionStart: Boolean,
    val ratingRequest: Boolean,
    val settlementDone: Boolean
)
