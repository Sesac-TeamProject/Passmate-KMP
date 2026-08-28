package org.sesacteamproject.passmate.session.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/{roomId}/session 응답 — contracts §재접속 프로토콜과 1:1
@Serializable
data class SessionSnapshotResponse(
    val status: String? = null,
    val ts: String,
    val questionCount: Int? = null,
    val currentQuestion: QuestionDto? = null,
    val myAnswers: List<AnswerDto> = emptyList(),
    val totalScore: Double? = null,
    val rank: Int? = null,
    val ranking: List<RankingEntryDto> = emptyList(),
    val isLocked: Boolean = false
) {

    @Serializable
    data class QuestionDto(
        val questionId: Long,
        val questionNo: Int,
        val type: String? = null,
        val body: String,
        val choices: List<String>? = null,
        val points: Int = 0,
        val timeLimitSec: Int = 0,
        val endsAt: String,
        val isClosed: Boolean = false
    )

    @Serializable
    data class AnswerDto(
        val questionId: Long,
        val correct: Boolean? = null,
        val earnedScore: Double? = null,
        val isProvisional: Boolean = false
    )

    @Serializable
    data class RankingEntryDto(
        val rank: Int,
        val participantId: Long,
        val nickname: String,
        val avatarId: Int? = null,
        val total: Double
    )
}
