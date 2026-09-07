package org.sesacteamproject.passmate.user.data.dto

import kotlinx.serialization.Serializable

// GET /users/me 응답 — 백엔드 실제 스키마(`MyProfileResponse`, 2026-09-07 로컬 확인) 기준.
// 계약 문서는 평면 구조로 적혀 있으나 서버는 카운트를 stats{}에 중첩해 주고,
// 코인은 coinBalance, 캐릭터는 defaultAvatarId(문자열 키)로 준다 — 계약 갱신 대상.
// 등급(level)은 이 응답에 없다. 등급은 GET /users/me/grade가 담당한다.
@Serializable
data class UserProfileResponse(
    val nickname: String = "",
    val email: String? = null,
    val joinedAt: String? = null,
    // 문자열 키("cat"·"fox"…) — 화면 인덱스 변환은 매퍼가 한다 (StudentAvatarKeys)
    val defaultAvatarId: String? = null,
    val coinBalance: Long? = null,
    val stats: StatsDto? = null
) {

    @Serializable
    data class StatsDto(
        val joinedRoomCount: Int? = null,
        val hostedRoomCount: Int? = null,
        val hostedSessionCount: Int? = null,
        val totalStudentCount: Int? = null
    )
}

// PUT /users/me 요청 — 서버 `UserProfileUpdateRequest`와 1:1. nickname은 필수다 (M-12-1·M-12-7)
@Serializable
data class UpdateProfileRequest(
    val nickname: String,
    val defaultAvatarId: String? = null
)

// GET/PUT /users/me/notification-settings — contracts §Users와 1:1
@Serializable
data class NotificationSettingsDto(
    val sessionStart: Boolean = true,
    val ratingRequest: Boolean = true,
    val settlementDone: Boolean = true
)
