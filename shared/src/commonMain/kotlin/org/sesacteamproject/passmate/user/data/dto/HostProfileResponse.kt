package org.sesacteamproject.passmate.user.data.dto

import kotlinx.serialization.Serializable
import org.sesacteamproject.passmate.payment.data.dto.PublicRoomDto

// GET /users/{userId}/profile 응답 — 백엔드 실제 스키마(`HostProfileResponse`, 2026-09-07 로컬 확인) 기준.
// 계약 문서는 이 엔드포인트를 "목만"으로 적어 두었으나 실제로는 동작한다.
// 이름이 계약과 다르다: avgStars→avgRating · roomCount→roomsHosted · rooms→openRooms.
// badges도 문자열 배열이 아니라 객체 배열이다 — 계약 갱신 대상.
@Serializable
data class HostProfileResponse(
    val userId: Long = 0,
    val nickname: String = "",
    val profileImageUrl: String? = null,
    // 문자열 키("cat"·"fox"…) — 화면 인덱스 변환은 매퍼가 한다
    val defaultAvatarId: String? = null,
    val level: Int? = null,
    val levelName: String? = null,
    val avgRating: Double? = null,
    val ratingCount: Int = 0,
    val roomsHosted: Int = 0,
    val totalStudents: Int = 0,
    val badges: List<BadgeDto> = emptyList(),
    val openRooms: List<PublicRoomDto> = emptyList()
) {

    @Serializable
    data class BadgeDto(
        val code: String = "",
        val achieved: Boolean = false
    )
}
