package org.sesacteamproject.passmate.report.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/{roomId}/reports/me 응답 — 계약 `LearningReportResponse`와 1:1.
// accuracy는 0~100 퍼센트다 (백엔드 ParticipantReport.accuracyOf).
@Serializable
data class LearningReportResponse(
    val roomId: Long = 0,
    val roomTitle: String = "",
    val participantId: Long = 0,
    val nickname: String = "",
    val totalQuestions: Int = 0,
    val correctCount: Int = 0,
    val accuracy: Double = 0.0,
    val totalScore: Int = 0,
    val finalRank: Int? = null,
    val weakTopics: List<String> = emptyList(),
    val improvementPoints: List<String> = emptyList(),
    // 반 평균 정답률(%) — 참가자 전원의 accuracy 평균 (백엔드 2026-09-08 확인, 프런트 계약 문서엔 미표기)
    val classAvgAccuracy: Double? = null,
    // 1위 정답률(%) — 시안 M-06에는 쓰이지 않아 도메인으로 옮기지 않는다
    val topAccuracy: Double? = null,
    // 주제별 맞은 수/전체 수 — 문항 순서대로 처음 나온 주제 순
    val topicAccuracy: List<TopicAccuracyDto> = emptyList(),
    val generatedAt: String? = null
) {

    @Serializable
    data class TopicAccuracyDto(
        val topic: String = "",
        val correctCount: Int = 0,
        val totalCount: Int = 0,
        val accuracy: Double = 0.0
    )
}
