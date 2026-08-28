package org.sesacteamproject.passmate.payment.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/public 목록 1건
@Serializable
data class PublicRoomDto(
    val roomId: Long = 0,
    val pin: String = "",
    val title: String = "",
    val topic: String? = null,
    val hostName: String = "",
    val hostLevel: Int? = null,
    val hostRating: Double? = null,
    val status: String? = null,
    val participantCount: Int? = null,
    val maxParticipants: Int? = null,
    val isPaid: Boolean = false,
    val entryFee: Int? = null,
    val scheduledAt: String? = null
)

@Serializable
data class PublicRoomPageResponse(
    val items: List<PublicRoomDto> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false
)
