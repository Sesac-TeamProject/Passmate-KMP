package org.sesacteamproject.passmate.rating.domain.model

// 제출 전 평가 입력 — 별점(1~5) + 태그 다중 + 한 줄 후기(선택) (FR-042)
data class RatingDraft(
    val stars: Int,
    val tags: Set<RatingTag>,
    val comment: String?
)
