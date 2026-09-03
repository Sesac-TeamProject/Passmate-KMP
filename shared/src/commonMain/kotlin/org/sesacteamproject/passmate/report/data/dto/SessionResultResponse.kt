package org.sesacteamproject.passmate.report.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/{roomId}/results/me 응답 — 계약 `MySessionResultResponse`와 1:1
@Serializable
data class SessionResultResponse(
    val roomId: Long = 0,
    val roomTitle: String = "",
    val status: String? = null,
    val endedAt: String? = null,
    val participantId: Long = 0,
    val nickname: String = "",
    val avatarId: String? = null,
    val guest: Boolean = false,
    val rank: Int? = null,
    val totalScore: Int = 0,
    val correctCount: Int = 0,
    val submitCount: Int = 0,
    val questionCount: Int = 0,
    val questions: List<AnswerResultDto> = emptyList(),
    val rating: RatingAvailabilityDto? = null
) {

    // 계약 `AnswerResultView` — 정답(answer)·해설은 종료 후에만 온다 (규칙 §13)
    @Serializable
    data class AnswerResultDto(
        val sessionQuestionId: Long = 0,
        val questionId: Long = 0,
        val orderNo: Int = 0,
        val type: String? = null,
        val content: String = "",
        val points: Int = 0,
        val answer: String? = null,
        val explanation: String? = null,
        // 내가 제출한 답
        val submitted: String? = null,
        val isCorrect: Boolean? = null,
        val score: Int = 0,
        // 첨삭까지 반영된 최종 점수
        val finalScore: Int? = null,
        val analysisStatus: String? = null,
        val analysis: EssayAnalysisDto? = null,
        val teacherReview: TeacherReviewDto? = null
    )

    // 계약 `EssayAnalysisView`
    @Serializable
    data class EssayAnalysisDto(
        val keyPoints: List<String> = emptyList(),
        val missingPoints: List<String> = emptyList(),
        val suggestions: String? = null,
        val summary: String? = null,
        val completedAt: String? = null
    )

    // 계약 `TeacherReviewView`
    @Serializable
    data class TeacherReviewDto(
        val comment: String = "",
        val adjustedScore: Int? = null,
        val improvement: String? = null,
        val reviewedAt: String? = null
    )

    // 계약 `RatingAvailability` — 평가 가능 여부는 서버가 판단한다
    @Serializable
    data class RatingAvailabilityDto(
        val available: Boolean = false,
        val blockedReason: String? = null,
        val alreadyRated: Boolean = false,
        val deadline: String? = null
    )
}
