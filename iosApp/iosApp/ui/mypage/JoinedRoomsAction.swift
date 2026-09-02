enum JoinedRoomsAction {
    case enter
    case retry
    case loadMore
    case clickRoomReport(roomId: Int64)
    case clickRejoin(pin: String)
    // 빈 상태 CTA — 홈 탭(=PIN 입장 폼)으로 보낸다 (규칙 §2-1-1)
    case clickEnterPin
    // 목록 불러오기 실패 화면의 "계속 안 되면 문의하기" (v6 E-List 실패 공통)
    case clickContactSupport
}
