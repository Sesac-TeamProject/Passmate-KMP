enum JoinedRoomsEvent {
    case requireSignIn
    case openReport(roomId: Int64)
    case rejoin(pin: String)
    // PIN 입장 폼(홈 탭) 열기 — 빈 상태 CTA (규칙 §2-1-1)
    case openPinEntry
    case showNotice(message: String)
}
