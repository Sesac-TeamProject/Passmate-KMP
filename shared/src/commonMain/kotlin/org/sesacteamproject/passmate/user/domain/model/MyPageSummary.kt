package org.sesacteamproject.passmate.user.domain.model

// 학습 기록 누적 요약 — 참여 횟수·평균 정답률·평균 순위·추이·보완 주제 (M-08 상단, FR-033)
data class MyPageSummary(
    val participationCount: Int,
    val accuracyPercent: Int,
    val avgRank: Double?,
    val trendText: String?,
    val weakTopics: List<String>
)
