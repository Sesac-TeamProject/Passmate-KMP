package org.sesacteamproject.passmate.report.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/{roomId}/reports/me 응답
@Serializable
data class LearningReportResponse(
    val accuracyPercent: Int = 0,
    val weakTopics: List<String> = emptyList(),
    val improvementPoints: List<String> = emptyList()
)
