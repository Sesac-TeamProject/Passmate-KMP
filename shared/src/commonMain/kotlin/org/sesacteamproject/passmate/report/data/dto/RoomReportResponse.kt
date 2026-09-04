package org.sesacteamproject.passmate.report.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/{roomId}/results 응답 — 실서버 실측 기준 (2026-09-04, OpenAPI /v3/api-docs).
// 계약 문서(contracts §결과·피드백·리포트)의 이름과 다르다: 계약은 roomTitle·dateLabel·
// students·accuracyPercent라 적혀 있으나 서버는 title·startedAt·participants·correctRate를 준다.
// 계약을 서버 실측에 맞춰 갱신해야 한다.
// pin은 서버가 주지 않는다 — 백엔드 요청 항목(docs/백엔드_전달사항.md)
@Serializable
data class RoomReportResponse(
    val roomId: Long = 0,
    val title: String = "",
    val status: String? = null,
    val startedAt: String? = null,
    val summary: SummaryDto = SummaryDto(),
    val questions: List<QuestionDto> = emptyList(),
    val participants: List<ParticipantDto> = emptyList()
) {

    @Serializable
    data class SummaryDto(
        val participantCount: Int = 0,
        val questionCount: Int = 0,
        // 0.0~1.0 비율이다 — 퍼센트 변환은 매퍼가 한다
        val avgCorrectRate: Double? = null,
        val avgScore: Double? = null,
        val aiAnalysisCount: Int = 0
    )

    @Serializable
    data class QuestionDto(
        val sessionQuestionId: Long = 0,
        val questionId: Long = 0,
        val orderNo: Int = 0,
        val type: String? = null,
        val content: String = "",
        val points: Int = 0,
        val submitCount: Int = 0,
        val correctCount: Int = 0,
        // 0.0~1.0 비율이다 — 서술형 미채점은 null
        val correctRate: Double? = null,
        val aiAnalysisCount: Int? = null
    )

    @Serializable
    data class ParticipantDto(
        val rank: Int? = null,
        val participantId: Long = 0,
        val nickname: String = "",
        val avatarId: String? = null,
        val totalScore: Double = 0.0,
        val correctCount: Int = 0,
        val submitCount: Int = 0
    )
}
