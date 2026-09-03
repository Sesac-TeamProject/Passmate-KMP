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
    // 서버가 판정한 게스트 입장 허용 여부 — 로그인 가드의 근거 (규칙 §8)
    val isGuestAllowed: Boolean,
    val host: RoomHost?
)
