package org.sesacteamproject.passmate.ui.hostroom

sealed interface RoomReportEvent {

    // 방 리포트는 호스트(회원) 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    data object RequireSignIn : RoomReportEvent

    data class ShareReport(val summary: String) : RoomReportEvent
}
