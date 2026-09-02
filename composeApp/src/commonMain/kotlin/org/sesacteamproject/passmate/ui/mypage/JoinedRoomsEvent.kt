package org.sesacteamproject.passmate.ui.mypage

sealed interface JoinedRoomsEvent {
    // 참여한 방은 회원 전용 — 딥링크 직접 진입 대비 보험 (탭 가드는 AppShellViewModel, 규칙 §8)
    data object RequireSignIn : JoinedRoomsEvent

    data class OpenReport(val roomId: Long) : JoinedRoomsEvent

    data class Rejoin(val pin: String) : JoinedRoomsEvent

    // PIN 입장 폼(홈 탭) 열기 — 빈 상태 CTA (규칙 §2-1-1)
    data object OpenPinEntry : JoinedRoomsEvent

    data class ShowNotice(val message: String) : JoinedRoomsEvent
}
