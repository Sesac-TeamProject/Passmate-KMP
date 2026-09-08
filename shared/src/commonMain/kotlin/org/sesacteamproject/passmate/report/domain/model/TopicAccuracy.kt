package org.sesacteamproject.passmate.report.domain.model

// 주제(개념)별 정답률 한 줄 — M-06 "개념별 정답률" 카드. 값은 전부 서버가 계산한다 (규칙 §1 서버 권위)
data class TopicAccuracy(
    val topic: String,
    val correctCount: Int,
    val totalCount: Int,
    val accuracyPercent: Int
)
