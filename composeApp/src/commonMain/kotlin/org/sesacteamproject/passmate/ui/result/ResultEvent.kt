package org.sesacteamproject.passmate.ui.result

sealed interface ResultEvent {

    // 내보내기(저장/공유) — 컨테이너가 플랫폼 공유 시트로 요약 텍스트를 넘긴다 (규칙 §7)
    data class ShareReport(val summary: String) : ResultEvent

    data object NavigateToSignup : ResultEvent

    data class ShowNotice(val message: String) : ResultEvent

    // 평가 완료 안내 (T080)
    data class RatingSubmitted(val message: String) : ResultEvent
}
