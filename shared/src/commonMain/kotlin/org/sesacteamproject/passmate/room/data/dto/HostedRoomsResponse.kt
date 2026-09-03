package org.sesacteamproject.passmate.room.data.dto

import kotlinx.serialization.Serializable

// GET /users/me/rooms/hosted 응답 — 계약 `HostedRoomsResponse`와 1:1.
// 서버가 진행 중(active)·종료(ended)를 나눠 주고 형태도 서로 다르다. 페이징은 없다.
@Serializable
data class HostedRoomsResponse(
    val reputation: ReputationDto = ReputationDto(),
    val active: List<ActiveRoomDto> = emptyList(),
    val ended: List<EndedRoomDto> = emptyList()
) {

    @Serializable
    data class ReputationDto(
        val level: Int = 1,
        val nextLevelProgress: Double = 0.0,
        val hostedSessionCount: Int = 0,
        val totalStudentCount: Int = 0,
        val averageStars: Double? = null,
        val ratingCount: Int = 0
    )

    @Serializable
    data class ActiveRoomDto(
        val roomId: Long = 0,
        val title: String = "",
        val pin: String = "",
        val status: String? = null,
        // LocalDateTime 직렬화 — "2026-08-29T20:00:00"
        val scheduledAt: String? = null,
        val startedAt: String? = null,
        val participantCount: Int? = null,
        val currentQuestionNo: Int = 0
    )

    // 종료 방에는 pin·status가 없다 — 화면도 종료 카드에서 PIN을 쓰지 않는다
    @Serializable
    data class EndedRoomDto(
        val roomId: Long = 0,
        val title: String = "",
        val endedAt: String? = null,
        val studentCount: Int? = null,
        // 0~100 퍼센트 (SessionService: correctCount * 100.0 / submitCount)
        val correctRate: Double? = null,
        val averageStars: Double? = null,
        val ratingCount: Int = 0
    )
}
