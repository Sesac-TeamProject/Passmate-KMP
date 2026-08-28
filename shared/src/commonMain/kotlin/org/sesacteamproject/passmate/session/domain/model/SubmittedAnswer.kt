package org.sesacteamproject.passmate.session.domain.model

// 스냅샷의 내 답변 상태 — 재접속·늦은 입장 복구용 (FR-022)
data class SubmittedAnswer(
    val questionId: Long,
    val correct: Boolean?,
    val earnedScore: Double?,
    val isProvisional: Boolean
)
