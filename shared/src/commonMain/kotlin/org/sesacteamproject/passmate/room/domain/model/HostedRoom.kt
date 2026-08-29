package org.sesacteamproject.passmate.room.domain.model

// 내가 만든 방 1건 (GET /users/me/rooms/hosted) — M-13 진행 중/종료 구분은 status로 한다
data class HostedRoom(
    val roomId: Long,
    val pin: String,
    val title: String,
    val status: RoomStatus,
    val participantCount: Int?,
    val scheduledAt: String?,
    val endedAtLabel: String?,
    val avgAccuracyPercent: Int?
) {

    val isOngoing: Boolean
        get() = status == RoomStatus.WAITING || status == RoomStatus.RUNNING
}

// 방 생성 결과 (POST /rooms) — PIN은 서버가 자동 발급한다 (FR-004)
data class CreatedRoom(
    val roomId: Long,
    val pin: String,
    val qrUrl: String?
)
