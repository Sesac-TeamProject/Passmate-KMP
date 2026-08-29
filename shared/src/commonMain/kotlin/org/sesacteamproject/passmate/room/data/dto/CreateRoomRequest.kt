package org.sesacteamproject.passmate.room.data.dto

import kotlinx.serialization.Serializable

// POST /rooms 요청 — contracts §Rooms와 1:1 (questionSetId는 CONFIRMED 세트만)
@Serializable
data class CreateRoomRequest(
    val title: String,
    val questionSetId: Long? = null,
    val topic: String? = null,
    val maxParticipants: Int? = null,
    val scheduledAt: String? = null,
    val isPaid: Boolean = false,
    val entryFee: Int? = null,
    val isListed: Boolean = true
)

@Serializable
data class CreateRoomResponse(
    val roomId: Long = 0,
    val pin: String = "",
    val qrUrl: String? = null
)
