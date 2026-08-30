package org.sesacteamproject.passmate.ui.mypage

sealed interface JoinedRoomsEvent {

    // 마이페이지는 회원 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    data object RequireSignIn : JoinedRoomsEvent

    data class OpenReport(val roomId: Long) : JoinedRoomsEvent

    data class Rejoin(val pin: String) : JoinedRoomsEvent

    // 코인·결제 내역 화면으로 이동 (US14, M-12)
    data object OpenCoinHistory : JoinedRoomsEvent

    // 내 명성·뱃지 상세 화면으로 이동 (M-09)
    data object OpenReputation : JoinedRoomsEvent

    // 내가 만든 방 화면으로 이동 (M-13)
    data object OpenHostedRooms : JoinedRoomsEvent

    // 정산 화면으로 이동 (M-T4)
    data object OpenEarnings : JoinedRoomsEvent

    // 설정 화면으로 이동 (M-12)
    data object OpenSettings : JoinedRoomsEvent

    data class ShowNotice(val message: String) : JoinedRoomsEvent
}
