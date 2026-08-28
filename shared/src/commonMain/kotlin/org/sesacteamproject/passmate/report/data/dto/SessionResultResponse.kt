package org.sesacteamproject.passmate.report.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/{roomId}/results/me 응답 — contracts §결과와 1:1 (2026-08-28 백엔드 명세서 기준)
@Serializable
data class SessionResultResponse(
    val roomTitle: String = "",
    val rank: Int? = null,
    val totalScore: Double = 0.0,
    val correctCount: Int = 0,
    val questionCount: Int = 0,
    val canRate: Boolean = false,
    val isGuest: Boolean = false,
    val questions: List<QuestionDto> = emptyList()
) {

    @Serializable
    data class QuestionDto(
        val questionId: Long,
        val questionNo: Int,
        val title: String = "",
        val type: String? = null,
        val verdict: String? = null,
        val myAnswer: String? = null,
        val correctAnswer: String? = null,
        val explanation: String? = null,
        val earnedScore: Double = 0.0,
        val aiFeedback: AiFeedbackDto? = null,
        val hostReview: HostReviewDto? = null
    )

    @Serializable
    data class AiFeedbackDto(
        val status: String? = null,
        val coveredConcepts: List<String> = emptyList(),
        val missingConcepts: List<String> = emptyList(),
        val weaknesses: String? = null,
        val improvement: String? = null,
        val suggestedScore: Double? = null
    )

    @Serializable
    data class HostReviewDto(
        val comment: String = "",
        val improvement: String? = null,
        val adjustedScore: Double? = null
    )
}
