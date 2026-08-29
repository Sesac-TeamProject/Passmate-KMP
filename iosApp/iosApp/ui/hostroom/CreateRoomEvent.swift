enum CreateRoomEvent {
    // 방 생성 완료 — PIN은 서버가 자동 발급 (FR-004)
    case created(pin: String)
    case showNotice(message: String)
}
