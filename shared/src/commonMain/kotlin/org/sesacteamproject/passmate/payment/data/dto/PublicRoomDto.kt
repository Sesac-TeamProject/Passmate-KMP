package org.sesacteamproject.passmate.payment.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/public 목록 1건 — 계약 `PublicRoomResponse`와 1:1.
// 서버는 non_null 직렬화라 값이 없는 필드는 키가 생략되므로 전부 기본값을 둔다.
@Serializable
data class PublicRoomDto(
    val id: Long = 0,
    val title: String = "",
    val topic: String? = null,
    val status: String? = null,
    // FREE · PAID · BRANDED
    val type: String? = null,
    val fee: Int? = null,
    val questionCount: Int? = null,
    val participantCount: Int? = null,
    val maxParticipants: Int? = null,
    val host: PublicRoomHostDto? = null,
    val scheduledAt: String? = null,
    val startedAt: String? = null
)

// 계약 `PublicRoomHostResponse` — userId·nickname만 온다 (등급·별점 없음)
@Serializable
data class PublicRoomHostDto(
    val userId: Long? = null,
    val nickname: String = ""
)

// 서버 공통 페이지 응답 — 커서가 아니라 page/size 기반이다
@Serializable
data class PublicRoomPageResponse(
    val content: List<PublicRoomDto> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val hasNext: Boolean = false
)
