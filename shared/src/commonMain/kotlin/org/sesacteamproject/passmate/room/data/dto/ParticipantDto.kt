package org.sesacteamproject.passmate.room.data.dto

import kotlinx.serialization.Serializable

// 계약 `ParticipantResponse` — GET /rooms/{roomId}/participants는 이 객체의 배열을 준다.
// avatarId는 문자열 키다(시안 "학생 아바타 — 키 이름").
@Serializable
data class ParticipantDto(
    val id: Long = 0,
    val nickname: String = "",
    val avatarId: String? = null,
    val isGuest: Boolean = false,
    val joinedAt: String? = null
)
