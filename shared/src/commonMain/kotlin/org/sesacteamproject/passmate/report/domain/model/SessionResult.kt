package org.sesacteamproject.passmate.report.domain.model

// 내 세션 결과 — 최종 점수·랭킹·정답 수 + 문항별 결과 (FR-030). 게스트는 열람용(가입 유도)
data class SessionResult(
    val roomTitle: String,
    val rank: Int?,
    val totalScore: Double,
    val correctCount: Int,
    // 내가 제출한 문항 수 — 정답 수와 다르다. 평가 시트 카드의 "내 제출 n/N" (M-06 v2)
    val submitCount: Int,
    val questionCount: Int,
    val questions: List<QuestionResult>,
    val canRate: Boolean,
    val isGuest: Boolean
)
