package org.sesacteamproject.passmate.user.data.dto

import kotlinx.serialization.Serializable
import org.sesacteamproject.passmate.payment.data.dto.PublicRoomDto

// GET /users/{userId}/profile 응답 — contracts §평가·등급과 1:1
@Serializable
data class HostProfileResponse(
    val userId: Long = 0,
    val nickname: String = "",
    val intro: String? = null,
    val level: Int? = null,
    val avgStars: Double? = null,
    val ratingCount: Int = 0,
    val roomCount: Int = 0,
    val totalStudents: Int = 0,
    val badges: List<String> = emptyList(),
    val rooms: List<PublicRoomDto> = emptyList()
)
