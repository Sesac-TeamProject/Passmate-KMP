package org.sesacteamproject.passmate.room.data.dto

import kotlinx.serialization.Serializable

// POST /rooms/{roomId}/participants 요청 — avatarId 미지정 시 서버 랜덤 부여
@Serializable
data class JoinRoomRequest(
    val nickname: String,
    val avatarId: Int? = null
)
