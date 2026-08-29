enum MyInfoEvent {
    case requireSignIn
    case openReport(roomId: Int64)
    case rejoin(pin: String)
    case openCoinHistory
    case openReputation
    case openHostedRooms
    case openEarnings
    case openSettings
    case showNotice(message: String)
}
