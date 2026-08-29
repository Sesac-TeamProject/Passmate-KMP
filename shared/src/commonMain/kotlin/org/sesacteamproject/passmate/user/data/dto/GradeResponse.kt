package org.sesacteamproject.passmate.user.data.dto

import kotlinx.serialization.Serializable

// GET /users/me/grade 응답 — contracts §평가·등급과 1:1
@Serializable
data class GradeResponse(
    val level: Int = 1,
    val achievedAt: String? = null,
    val stats: StatsDto = StatsDto(),
    val next: NextDto? = null
) {

    @Serializable
    data class StatsDto(
        val participationCount: Int = 0,
        val avgAccuracyPercent: Int? = null,
        val roomCount: Int = 0,
        val totalStudents: Int = 0,
        val avgStars: Double? = null,
        val ratingCount: Int = 0
    )

    @Serializable
    data class NextDto(
        val level: Int = 2,
        val progressPercent: Int = 0,
        val criteria: List<CriterionDto> = emptyList()
    )

    @Serializable
    data class CriterionDto(
        val label: String = "",
        val current: Double = 0.0,
        val target: Double = 0.0,
        val met: Boolean = false
    )
}
