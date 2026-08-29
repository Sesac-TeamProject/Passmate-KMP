package org.sesacteamproject.passmate.payment.domain.model

import org.sesacteamproject.passmate.room.domain.model.RoomStatus

// 공개 방 목록 1건 (GET /rooms/public) — 홈 인기 방·탐색(M-11) 카드
data class PublicRoom(
    val roomId: Long,
    val pin: String,
    val title: String,
    val topic: String?,
    // 호스트 userId — 선생님 프로필 시트(M-10) 진입용 (contracts 2026-08-29 추가)
    val hostId: Long?,
    val hostName: String,
    val hostLevel: Int?,
    val hostRating: Double?,
    val status: RoomStatus,
    val participantCount: Int?,
    val maxParticipants: Int?,
    val isPaid: Boolean,
    val entryFee: Int?,
    val scheduledAt: String?
)

// 정렬: 인기(참여 인원·운영 중 우선) / 시작 예정
enum class RoomSort(val wireValue: String) {

    POPULAR("popular"),
    UPCOMING("upcoming")
}

// 방 유형 필터: 전체 / 무료 / 유료
enum class RoomTypeFilter(val wireValue: String) {

    ALL("all"),
    FREE("free"),
    PAID("paid")
}
