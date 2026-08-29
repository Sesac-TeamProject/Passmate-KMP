package org.sesacteamproject.passmate.question.data.dto

import kotlinx.serialization.Serializable

// GET /question-sets 응답 — contracts §Question Sets와 1:1
@Serializable
data class QuestionSetsResponse(
    val items: List<QuestionSetDto> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false
) {

    @Serializable
    data class QuestionSetDto(
        val setId: Long = 0,
        val title: String = "",
        val status: String? = null,
        val questionCount: Int = 0,
        val usedCount: Int? = null,
        val lastUsedAt: String? = null,
        val createdAt: String? = null
    )
}
