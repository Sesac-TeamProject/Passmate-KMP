package org.sesacteamproject.passmate.user.data.dto

import kotlinx.serialization.Serializable

// GET /users/me/rooms/joined 응답 — 요약+진행중+참여 방 목록 (FR-032·033)
@Serializable
data class MyPageResponse(
    val summary: SummaryDto = SummaryDto(),
    val ongoing: OngoingDto? = null,
    val rooms: List<RoomDto> = emptyList(),
    val nextCursor: String? = null
) {

    @Serializable
    data class SummaryDto(
        val participationCount: Int = 0,
        val accuracyPercent: Int = 0,
        val avgRank: Double? = null,
        val trendText: String? = null,
        val weakTopics: List<String> = emptyList()
    )

    @Serializable
    data class OngoingDto(
        val roomId: Long,
        val pin: String,
        val title: String,
        val hostNickname: String? = null,
        val progressLabel: String? = null
    )

    @Serializable
    data class RoomDto(
        val roomId: Long,
        val title: String,
        val dateLabel: String = "",
        val questionCount: Int = 0,
        val myScore: Double? = null,
        val myRank: Int? = null,
        val hasReport: Boolean = false
    )
}
