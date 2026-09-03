package org.sesacteamproject.passmate.core.network.event

import kotlinx.serialization.Serializable

// contracts/websocket-events.md의 이벤트 data와 1:1 — 계약에 없는 필드 임의 추가 금지 (규칙 §13)
// 이벤트명·participantId 표기는 2026-08-28 백엔드 API 명세서 기준으로 정합화됨
sealed interface ServerEvent {

    @Serializable
    data class ParticipantJoined(
        val participantId: Long,
        val nickname: String,
        val isGuest: Boolean,
        val avatarId: Int? = null,
        val count: Int
    ) : ServerEvent

    @Serializable
    data class ParticipantLeft(
        val participantId: Long,
        val count: Int,
        val reason: String? = null
    ) : ServerEvent {

        companion object {
            const val REASON_LEFT = "LEFT"
            const val REASON_KICKED = "KICKED"
        }
    }

    @Serializable
    // 서버는 이 이벤트에 페이로드를 싣지 않는다 (백엔드 SessionService)
    data class SessionStarted(
        val sessionId: Long = 0,
        val questionCount: Int = 0
    ) : ServerEvent

    @Serializable
    data class QuestionStarted(
        val questionId: Long,
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
            val participantId: Long,
            val delta: Double,
            val total: Double
        )
    }

    @Serializable
    data class RankingUpdated(
        val ranking: List<RankingEntry>
    ) : ServerEvent

    @Serializable
    data class ScreenLocked(
        val locked: Boolean
    ) : ServerEvent

    @Serializable
    data class HintPublished(
        val hintId: Long,
        val questionNo: Int,
        val clipUrl: String,
        val durationMs: Long
    ) : ServerEvent

    @Serializable
    // 서버는 최종 랭킹 배열만 보낸다
    data class SessionEnded(
        val sessionId: Long = 0,
        val finalRanking: List<RankingEntry> = emptyList(),
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
    data class FeedbackReady(
        val answerId: Long,
        val questionNo: Int
    ) : ServerEvent

    @Serializable
    data class FeedbackFailed(
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
        val participantId: Long,
        val nickname: String,
        val avatarId: Int? = null,
        val total: Double
    )

    // ── 호스트 전용 (/topic/rooms/{roomId}/host — M-T2 리모컨·프로젝터 웹) ──

    @Serializable
    data class SubmissionUpdated(
        val questionNo: Int,
        val submittedCount: Int,
        val totalCount: Int
    ) : ServerEvent

    // 프로젝터 웹 화면 접속/해제 — 리모컨 상단 동기화 상태 표시
    @Serializable
    data object ProjectorConnected : ServerEvent

    @Serializable
    data object ProjectorDisconnected : ServerEvent
}
