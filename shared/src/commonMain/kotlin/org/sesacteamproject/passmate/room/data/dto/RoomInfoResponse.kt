package org.sesacteamproject.passmate.room.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/pin/{pin} 응답 — 계약 `RoomSummaryResponse`와 1:1.
// pin·host·questionCount는 서버가 주지 않는다. pin은 조회 키라 호출부가 채운다.
@Serializable
data class RoomInfoResponse(
    val id: Long = 0,
    val title: String = "",
    val topic: String? = null,
    val status: String? = null,
    // FREE · PAID · BRANDED
    val type: String? = null,
    val fee: Int? = null,
    val participantCount: Int? = null,
    val maxParticipants: Int? = null,
    // 게스트(비로그인) 입장 허용 여부 — 유료 방 로그인 가드(규칙 §8)의 서버 판정
    val guestAllowed: Boolean = true
)
