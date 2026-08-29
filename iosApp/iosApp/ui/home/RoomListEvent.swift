enum RoomListEvent {
    case openRoom(pin: String)
    case openPinEntry
    // 선생님 프로필 시트 열기 (M-10) — 시트 표시는 화면이 소유한다 (규칙 §11-1)
    case openHostProfile(hostId: Int64)
    case showNotice(message: String)
}
