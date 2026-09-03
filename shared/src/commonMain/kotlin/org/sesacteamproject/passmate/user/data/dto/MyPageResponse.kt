package org.sesacteamproject.passmate.user.data.dto

import kotlinx.serialization.Serializable

// GET /users/me/rooms/joined 응답 — 계약 `JoinedRoomsResponse`와 1:1.
// summary + rooms(page 응답) 중첩 구조다. 진행 중 방·추이 문구는 서버가 주지 않는다.
@Serializable
data class MyPageResponse(
    val summary: SummaryDto = SummaryDto(),
    val rooms: RoomPageDto = RoomPageDto()
) {

    @Serializable
    data class SummaryDto(
        val completedSessionCount: Int = 0,
        // 0~100 퍼센트 (백엔드 ParticipantReport.accuracyOf)
        val averageAccuracy: Double = 0.0,
        val averageRank: Double = 0.0,
        val weakTopics: List<String> = emptyList()
    )

    @Serializable
    data class RoomPageDto(
        val content: List<RoomDto> = emptyList(),
        val page: Int = 0,
        val size: Int = 0,
        val totalElements: Long = 0,
        val totalPages: Int = 0,
        val hasNext: Boolean = false
    )

    @Serializable
    data class RoomDto(
        val roomId: Long = 0,
        val title: String = "",
        val hostNickname: String? = null,
        val status: String? = null,
        // LocalDateTime 직렬화 — "2026-07-18T21:10:00"
        val startedAt: String? = null,
        val endedAt: String? = null,
        val questionCount: Int = 0,
        val fee: Int? = null,
        val myScore: Int? = null,
        val myRank: Int? = null,
        val myAccuracy: Double? = null,
        val hasReport: Boolean = false
    )
}
