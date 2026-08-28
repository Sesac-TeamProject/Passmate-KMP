package org.sesacteamproject.passmate.session.domain.model

// 답안 제출 응답 — 채점·순위는 전부 서버 계산 값을 렌더링만 한다 (규칙 §1)
data class AnswerResult(
    val correct: Boolean?,
    val baseScore: Double,
    val speedBonus: Double,
    val earnedScore: Double,
    val totalScore: Double,
    val rank: Int?,
    val rankDelta: Int?,
    val isProvisional: Boolean
)
