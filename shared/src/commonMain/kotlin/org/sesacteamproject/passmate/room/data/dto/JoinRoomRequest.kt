package org.sesacteamproject.passmate.room.data.dto

import kotlinx.serialization.Serializable

// POST /rooms/{roomId}/participants 요청 — 계약 `JoinRoomRequest`.
// avatarId를 비우면 서버가 기본 캐릭터를 쓴다.
@Serializable
data class JoinRoomRequest(
    val nickname: String,
    val avatarId: String? = null
)
