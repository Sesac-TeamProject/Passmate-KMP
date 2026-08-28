package org.sesacteamproject.passmate.room.data.dto

import kotlinx.serialization.Serializable

// POST /rooms/{roomId}/participants 응답 — participantToken은 게스트에게만 발급
@Serializable
data class JoinRoomResponse(
    val participantId: Long,
    val participantToken: String? = null,
    val avatarId: Int? = null
)
