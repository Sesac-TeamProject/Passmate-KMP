package org.sesacteamproject.passmate.report.domain.model

// 내 세션 결과 — 최종 점수·랭킹·정답 수 + 문항별 결과 (FR-030). 게스트는 열람용(가입 유도)
data class SessionResult(
    val roomTitle: String,
    val rank: Int?,
    val totalScore: Double,
    val correctCount: Int,
    val questionCount: Int,
    val questions: List<QuestionResult>,
    val canRate: Boolean,
    val isGuest: Boolean
)
