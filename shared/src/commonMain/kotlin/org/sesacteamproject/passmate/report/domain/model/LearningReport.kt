package org.sesacteamproject.passmate.report.domain.model

// 학습 리포트 — 정답률·취약(보완할) 주제·개선 포인트 (GET /rooms/{roomId}/reports/me, FR-030·033)
data class LearningReport(
    val accuracyPercent: Int,
    val weakTopics: List<String>,
    val improvementPoints: List<String>
)
