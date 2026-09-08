package org.sesacteamproject.passmate.rating.domain.model

// 평가 태그 다중 선택 (백엔드 고정 5종) — 이름·라벨 모두 서버 kr.passmate.rating.domain.RatingTag와 1:1.
// 이름이 어긋나면 서버가 enum 파싱에 실패해 평가 전송이 통째로 막힌다.
enum class RatingTag(val wireValue: String, val label: String) {
    CLEAR_EXPLANATION("CLEAR_EXPLANATION", "설명이 명확해요"),
    FAIR_DIFFICULTY("FAIR_DIFFICULTY", "난이도가 적당해요"),
    GOOD_PACING("GOOD_PACING", "시간 배분이 좋아요"),
    HELPFUL_HINT("HELPFUL_HINT", "힌트가 도움됐어요"),
    GOOD_QUESTIONS("GOOD_QUESTIONS", "문제 품질이 좋아요");

    companion object {

        val all: List<RatingTag> = entries
    }
}
