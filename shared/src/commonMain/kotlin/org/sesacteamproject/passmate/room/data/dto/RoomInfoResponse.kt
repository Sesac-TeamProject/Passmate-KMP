package org.sesacteamproject.passmate.room.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/pin/{pin} 응답 — contracts §Rooms와 1:1
@Serializable
data class RoomInfoResponse(
    val roomId: Long,
    val pin: String,
    val title: String,
    val topic: String? = null,
    val status: String? = null,
    val questionCount: Int? = null,
    val estimatedMinutes: Int? = null,
    val scheduledAt: String? = null,
    val participantCount: Int? = null,
    val maxParticipants: Int? = null,
    val isPaid: Boolean = false,
    val entryFee: Int? = null,
    val host: Host? = null
) {

    @Serializable
    data class Host(
        val userId: Long? = null,
        val nickname: String,
        val level: Int? = null,
        val avgStars: Double? = null,
        val ratingCount: Int? = null
    )
}
