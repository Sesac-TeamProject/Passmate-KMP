package org.sesacteamproject.passmate.room.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/{roomId} 응답 — 계약 `RoomResponse`와 1:1.
// 공개 방 목록(`PublicRoomResponse`)에는 pin이 없어서, 방 카드를 눌렀을 때
// 여기서 pin을 얻어 Join 라우트(`join?pin=`)로 넘긴다.
@Serializable
data class RoomDetailResponse(
    val id: Long = 0,
    val pin: String = "",
    val title: String = "",
    val description: String? = null,
    val topic: String? = null,
    val status: String? = null,
    val type: String? = null,
    val fee: Int? = null,
    val questionSetId: Long? = null,
    val hostUserId: Long? = null,
    val maxParticipants: Int? = null,
    val participantCount: Int? = null,
    val isPublic: Boolean = false,
    val screenLocked: Boolean = false,
    val currentQuestionNo: Int = 0,
    val scheduledAt: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null
)
