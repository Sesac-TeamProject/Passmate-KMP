package org.sesacteamproject.passmate.session.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/{roomId}/session 응답 — 계약 `SessionSnapshotResponse`와 1:1.
// 서버 시각(ts)은 본문에 없다 — 응답 HTTP Date 헤더를 쓴다(매퍼 인자).
// 문항별 내 답변 목록도 없고 제출 여부(submitted)만 온다.
@Serializable
data class SessionSnapshotResponse(
    val roomId: Long = 0,
    val status: String? = null,
    val currentQuestionNo: Int = 0,
    val totalCount: Int = 0,
    val screenLocked: Boolean = false,
    val currentQuestion: QuestionDto? = null,
    val submitted: Boolean = false,
    val ranking: List<RankingEntryDto> = emptyList()
) {

    // 계약 `QuestionStartedPayload` — 정답은 오지 않는다 (정답은 QUESTION_ENDED에서만, 규칙 §13)
    @Serializable
    data class QuestionDto(
        val sessionQuestionId: Long = 0,
        val questionId: Long = 0,
        val orderNo: Int = 0,
        val totalCount: Int = 0,
        val type: String? = null,
        val content: String = "",
        val choices: List<String> = emptyList(),
        val points: Int = 0,
        val timeLimitSec: Int = 0,
        val endsAt: String = ""
    )

    // 계약 `RankingEntry` — avatarId는 문자열 키다
    @Serializable
    data class RankingEntryDto(
        val rank: Int = 0,
        val participantId: Long = 0,
        val nickname: String = "",
        val avatarId: String? = null,
        val totalScore: Int = 0
    )
}
