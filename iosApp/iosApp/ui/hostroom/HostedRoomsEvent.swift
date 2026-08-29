enum HostedRoomsEvent {
    // 내가 만든 방은 회원 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    case requireSignIn
    case openCreateSheet
    case openReputation
    case showNotice(message: String)
}
