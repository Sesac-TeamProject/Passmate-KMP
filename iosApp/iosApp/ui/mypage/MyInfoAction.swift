enum MyInfoAction {
    case enter
    case retry
    case loadMore
    case clickRoomReport(roomId: Int64)
    case clickRejoin(pin: String)
    case clickCoinHistory
}
