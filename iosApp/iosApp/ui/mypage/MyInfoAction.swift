enum MyInfoAction {
    case enter
    case retry
    case loadMore
    case clickRoomReport(roomId: Int64)
    case clickRejoin(pin: String)
    case clickCoinHistory
    case clickReputation
    case clickHostedRooms
    case clickEarnings
    case clickSettings
}
