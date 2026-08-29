package org.sesacteamproject.passmate.report.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/{roomId}/results 응답 — contracts §결과·피드백·리포트와 1:1
@Serializable
data class RoomReportResponse(
    val roomTitle: String = "",
    val pin: String = "",
    val status: String? = null,
    val dateLabel: String? = null,
    val summary: SummaryDto = SummaryDto(),
    val questions: List<QuestionDto> = emptyList(),
    val students: List<StudentDto> = emptyList()
) {

    @Serializable
    data class SummaryDto(
        val avgAccuracyPercent: Int? = null,
        val studentCount: Int = 0,
        val questionCount: Int = 0,
        val aiAnalysisCount: Int = 0,
        val avgScore: Double? = null,
        val topScore: Double? = null
    )

    @Serializable
    data class QuestionDto(
        val questionId: Long = 0,
        val questionNo: Int = 0,
        val title: String = "",
        val type: String? = null,
        val accuracyPercent: Int? = null,
        val aiFeedbackCount: Int? = null
    )

    @Serializable
    data class StudentDto(
        val participantId: Long = 0,
        val nickname: String = "",
        val rank: Int? = null,
        val totalScore: Double = 0.0,
        val correctCount: Int = 0,
        val isGuest: Boolean = false
    )
}
