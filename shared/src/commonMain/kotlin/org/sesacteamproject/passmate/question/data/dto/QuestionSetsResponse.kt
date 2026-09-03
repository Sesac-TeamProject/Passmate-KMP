package org.sesacteamproject.passmate.question.data.dto

import kotlinx.serialization.Serializable

// GET /question-sets 응답 — 계약 `PageResponse<QuestionSetSummaryResponse>`와 1:1.
// 커서가 아니라 page/size 기반이다.
@Serializable
data class QuestionSetsResponse(
    val content: List<QuestionSetDto> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val hasNext: Boolean = false
) {

    @Serializable
    data class QuestionSetDto(
        val id: Long = 0,
        val title: String = "",
        val description: String? = null,
        val status: String? = null,
        // MANUAL · AI 등 출처
        val source: String? = null,
        val questionCount: Int = 0,
        val totalPoints: Int = 0,
        val estimatedSeconds: Int = 0,
        val usageCount: Int? = null,
        val lastUsedAt: String? = null,
        val confirmedAt: String? = null,
        val createdAt: String? = null
    )
}
