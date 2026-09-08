package org.sesacteamproject.passmate.report.domain.model

// 학습 리포트 — 정답률·취약(보완할) 주제·개선 포인트·반 평균·주제별 정답률 (GET /rooms/{roomId}/reports/me, FR-030·033)
data class LearningReport(
    val accuracyPercent: Int,
    val weakTopics: List<String>,
    val improvementPoints: List<String>,
    // 반 평균 정답률(%) — 서버가 안 주면 null이고 M-06 "반 평균 대비" 카드를 숨긴다
    val classAverageAccuracyPercent: Int?,
    // 주제별 정답률 — 주제 없는 문항만 있으면 비어 있고 M-06 "개념별 정답률" 카드를 숨긴다
    val topicAccuracies: List<TopicAccuracy>
)
