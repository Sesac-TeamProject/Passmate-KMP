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
    val generatedAt: String? = null
)
