package org.sesacteamproject.passmate.user.data.dto

import kotlinx.serialization.Serializable

// GET /users/me/grade 응답 — 계약 `HostGradeResponse`와 1:1.
// 서버는 중첩 없이 평면 구조로 주고, 참여 횟수·정답률은 이 응답에 없다.
@Serializable
data class GradeResponse(
    val level: Int = 1,
    val levelName: String? = null,
    val levelAchievedAt: String? = null,
    val roomsHosted: Int = 0,
    val totalStudents: Int = 0,
    val avgRating: Double? = null,
    val ratingCount: Int = 0,
    val nextLevel: Int? = null,
    val nextLevelName: String? = null,
    val nextRequirements: List<RequirementDto> = emptyList(),
    // 0.0~1.0 비율 (조건별 달성 비율의 평균, 1.0으로 잘림)
    val nextLevelProgress: Double? = null,
    val ratingSamplePending: Boolean = false,
    val unlocked: List<String> = emptyList(),
    val lastEvaluatedAt: String? = null
) {

    @Serializable
    data class RequirementDto(
        // ROOMS_HOSTED · TOTAL_STUDENTS · AVG_RATING
        val type: String? = null,
        val label: String = "",
        val current: Double = 0.0,
        val target: Double = 0.0,
        val met: Boolean = false
    )
}
