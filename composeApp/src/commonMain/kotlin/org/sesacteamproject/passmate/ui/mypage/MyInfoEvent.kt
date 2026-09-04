package org.sesacteamproject.passmate.ui.mypage

sealed interface MyInfoEvent {
    // 마이는 회원 전용 — 딥링크 직접 진입 대비 보험 (탭 가드는 AppShellViewModel, 규칙 §8)
    data object RequireSignIn : MyInfoEvent

    data object OpenReputation : MyInfoEvent

    data object OpenEditProfile : MyInfoEvent

    data object OpenPaymentMethod : MyInfoEvent

    data object OpenCoinHistory : MyInfoEvent

    // 코인 충전 화면 (M-12-4·M-12-6)
    data object OpenCharge : MyInfoEvent

    data object OpenSettlementAccount : MyInfoEvent

    data object OpenEarnings : MyInfoEvent

    data object OpenNotifications : MyInfoEvent

    // card/기타 회원 탈퇴 행 → 전용 화면 push (M-12-12)
    data object OpenDeleteAccount : MyInfoEvent

    // 로그아웃 완료 → 홈 탭으로 (세션 정리는 shared가 수행)
    data object SignedOut : MyInfoEvent

    data class ShowNotice(val message: String) : MyInfoEvent
}
