package org.sesacteamproject.passmate.rating.domain.model

// 평가 태그 다중 선택 (백엔드 명세서 고정 5종) — wire 값은 서버와 합의한 상수, 라벨은 표시용
enum class RatingTag(val wireValue: String, val label: String) {
    CLEAR_EXPLANATION("CLEAR_EXPLANATION", "설명이 명확해요"),
    GOOD_DIFFICULTY("GOOD_DIFFICULTY", "난이도가 적당해요"),
    GOOD_PACING("GOOD_PACING", "시간 배분이 좋아요"),
    HELPFUL_HINTS("HELPFUL_HINTS", "힌트가 도움됐어요"),
    GOOD_QUALITY("GOOD_QUALITY", "문제 품질이 좋아요");

    companion object {

        val all: List<RatingTag> = entries
    }
}
