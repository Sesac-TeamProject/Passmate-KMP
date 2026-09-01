enum JoinedRoomsAction {
    case enter
    case retry
    case loadMore
    case clickRoomReport(roomId: Int64)
    case clickRejoin(pin: String)
    // 목록 불러오기 실패 화면의 "계속 안 되면 문의하기" (v6 E-List 실패 공통)
    case clickContactSupport
}
