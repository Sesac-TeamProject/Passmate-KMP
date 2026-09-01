package org.sesacteamproject.passmate.ui.payment

sealed interface EarningsEvent {

    // 정산은 호스트(회원) 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    data object RequireSignIn : EarningsEvent

    data object OpenAccountSheet : EarningsEvent

    // 빈 상태 CTA — 「내가 만든 방」 탭으로 보낸다 (방 개설 진입점이 그 탭의 FAB다)
    data object OpenHostedRooms : EarningsEvent

    data class ShowNotice(val message: String) : EarningsEvent
}
