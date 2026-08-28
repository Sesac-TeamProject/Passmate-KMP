package org.sesacteamproject.passmate.report.domain.model

// 선생님 첨삭 — 최종 점수는 첨삭 보정 우선(ADJUSTED). 입력 화면은 파트2 T072, 여기선 표시만 (FR-034~035)
data class HostReview(
    val comment: String,
    val improvement: String?,
    val adjustedScore: Double?
)
