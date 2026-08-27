package org.sesacteamproject.passmate.core.network.event

import kotlinx.serialization.Serializable

// contracts/websocket-events.md의 이벤트 data와 1:1 — 계약에 없는 필드 임의 추가 금지 (규칙 §13)
sealed interface ServerEvent {

    @Serializable
    data class ParticipantJoined(
        val participationId: Long,
        val nickname: String,
        val isGuest: Boolean,
        val count: Int
    ) : ServerEvent

    @Serializable
    data class ParticipantLeft(
        val participationId: Long,
        val count: Int
    ) : ServerEvent

    @Serializable
    data class GameStarted(
        val sessionId: Long,
        val questionCount: Int
    ) : ServerEvent

    @Serializable
    data class QuestionStarted(
        val questionNo: Int,
        val type: String,
        val body: String,
        val choices: List<String>? = null,
        val points: Int,
        val timeLimitSec: Int,
        val endsAt: String
    ) : ServerEvent

    @Serializable
    data class AnswerSubmitted(
        val questionNo: Int,
        val submittedCount: Int,
        val totalCount: Int
    ) : ServerEvent

    @Serializable
    data class QuestionEnded(
        val questionNo: Int,
        val answerReveal: AnswerReveal,
        val correctCount: Int
    ) : ServerEvent {

        @Serializable
        data class AnswerReveal(
            val answer: String? = null,
            val explanation: String? = null
        )
    }

    @Serializable
    data class ScoreUpdated(
        val questionNo: Int,
        val scores: List<Entry>
    ) : ServerEvent {

        @Serializable
        data class Entry(
            val participationId: Long,
            val delta: Double,
            val total: Double
        )
    }

    @Serializable
    data class RankingUpdated(
        val ranking: List<RankingEntry>
    ) : ServerEvent

    @Serializable
    data class VoiceHint(
        val hintId: Long,
        val questionNo: Int,
        val clipUrl: String,
        val durationMs: Long
    ) : ServerEvent

    @Serializable
    data class GameFinished(
        val sessionId: Long,
        val finalRanking: List<RankingEntry>,
        val reportReady: Boolean = false
    ) : ServerEvent

    @Serializable
    data class ReportReady(
        val sessionId: Long
    ) : ServerEvent

    @Serializable
    data class RoomCancelled(
        val reason: String? = null
    ) : ServerEvent

    @Serializable
    data class AiFeedbackReady(
        val answerId: Long,
        val questionNo: Int
    ) : ServerEvent

    @Serializable
    data class AiFeedbackFailed(
        val answerId: Long,
        val questionNo: Int
    ) : ServerEvent

    @Serializable
    data class ReviewReceived(
        val answerId: Long,
        val questionNo: Int
    ) : ServerEvent

    @Serializable
    data class RankingEntry(
        val rank: Int,
        val participationId: Long,
        val nickname: String,
        val total: Double
    )
}
