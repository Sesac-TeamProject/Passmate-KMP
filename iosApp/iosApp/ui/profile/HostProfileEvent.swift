enum HostProfileEvent {
    // 차단은 회원 전용 — 게스트는 로그인 유도 (규칙 §8)
    case requireSignIn
    case joinRoom(pin: String)
    // 차단 완료 — 시트를 닫고 목록을 새로고침한다 (차단 호스트의 방은 공개 목록에서 숨김)
    case blockedAndClose
    case showNotice(message: String)
}
