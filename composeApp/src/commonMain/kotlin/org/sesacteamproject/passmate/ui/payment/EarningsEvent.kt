package org.sesacteamproject.passmate.ui.payment

sealed interface EarningsEvent {

    // 정산은 호스트(회원) 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    data object RequireSignIn : EarningsEvent

    data object OpenAccountSheet : EarningsEvent

    data class ShowNotice(val message: String) : EarningsEvent
}
