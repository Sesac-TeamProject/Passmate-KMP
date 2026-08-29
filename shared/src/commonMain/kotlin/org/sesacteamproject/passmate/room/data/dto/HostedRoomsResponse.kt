package org.sesacteamproject.passmate.room.data.dto

import kotlinx.serialization.Serializable

// GET /users/me/rooms/hosted 응답 — contracts §Rooms와 1:1
@Serializable
data class HostedRoomsResponse(
    val items: List<HostedRoomDto> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false
) {

    @Serializable
    data class HostedRoomDto(
        val roomId: Long = 0,
        val pin: String = "",
        val title: String = "",
        val status: String? = null,
        val participantCount: Int? = null,
        val scheduledAt: String? = null,
        val endedAtLabel: String? = null,
        val avgAccuracyPercent: Int? = null
    )
}
