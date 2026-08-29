enum HostedRoomsEvent {
    // 내가 만든 방은 회원 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    case requireSignIn
    case openCreateSheet
    case openReputation
    // 종료된 방 상세 → 방 리포트 (M-14)
    case openRoomReport(roomId: Int64)
    case showNotice(message: String)
}
