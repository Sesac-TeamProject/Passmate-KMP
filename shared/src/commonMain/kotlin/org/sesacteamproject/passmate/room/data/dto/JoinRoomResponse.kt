package org.sesacteamproject.passmate.room.data.dto

import kotlinx.serialization.Serializable

// POST /rooms/{roomId}/participants 응답 — 참가자 정보와 토큰이 함께 온다.
// accessToken은 게스트(비로그인) 입장 시 발급되는 게스트 토큰이다 (규칙 §8).
@Serializable
data class JoinRoomResponse(
    val participant: ParticipantDto = ParticipantDto(),
    val accessToken: String? = null
)
