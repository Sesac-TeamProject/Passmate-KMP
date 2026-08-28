package org.sesacteamproject.passmate.room.domain.model

// GET /rooms/pin/{pin} 입장 전 방 정보 (contracts §Rooms)
data class RoomInfo(
    val roomId: Long,
    val pin: String,
    val title: String,
    val topic: String?,
    val status: RoomStatus,
    val questionCount: Int?,
    val estimatedMinutes: Int?,
    val scheduledAt: String?,
    val participantCount: Int?,
    val maxParticipants: Int?,
    val isPaid: Boolean,
    val entryFee: Int?,
    val host: RoomHost?
)
