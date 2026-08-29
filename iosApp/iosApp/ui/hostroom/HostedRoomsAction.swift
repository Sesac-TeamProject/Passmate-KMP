enum HostedRoomsAction {
    case enter
    case retry
    case loadMore
    // + FAB → 새 방 만들기 시트 (시트 표시는 화면이 소유)
    case clickCreate
    case clickReputation
    case clickOngoingRoom(roomId: Int64, pin: String)
    case clickEndedRoom(roomId: Int64)
    // 시트에서 방 생성 완료 — 목록 새로고침 + PIN 안내
    case roomCreated(pin: String)
    case notice(message: String)
}
