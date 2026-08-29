package org.sesacteamproject.passmate.ui.mypage

sealed interface MyInfoEvent {

    // 마이페이지는 회원 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    data object RequireSignIn : MyInfoEvent

    data class OpenReport(val roomId: Long) : MyInfoEvent

    data class Rejoin(val pin: String) : MyInfoEvent

    // 코인·결제 내역 화면으로 이동 (US14, M-12)
    data object OpenCoinHistory : MyInfoEvent

    // 내 명성·뱃지 상세 화면으로 이동 (M-09)
    data object OpenReputation : MyInfoEvent

    // 내가 만든 방 화면으로 이동 (M-13)
    data object OpenHostedRooms : MyInfoEvent

    // 정산 화면으로 이동 (M-T4)
    data object OpenEarnings : MyInfoEvent

    data class ShowNotice(val message: String) : MyInfoEvent
}
