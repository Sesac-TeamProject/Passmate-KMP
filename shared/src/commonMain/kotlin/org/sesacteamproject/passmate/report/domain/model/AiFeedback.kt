package org.sesacteamproject.passmate.report.domain.model

// 서술형 AI 분석 결과 (contracts §AI 분석 스키마) — 분석 실패는 에러가 아니라 상태로 표시한다 (규칙 §10)
data class AiFeedback(
    val status: AiFeedbackStatus,
    val coveredConcepts: List<String>,
    val missingConcepts: List<String>,
    val weaknesses: String?,
    val improvement: String?,
    val suggestedScore: Double?
)
