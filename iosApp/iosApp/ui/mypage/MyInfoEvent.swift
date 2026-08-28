enum MyInfoEvent {
    case requireSignIn
    case openReport(roomId: Int64)
    case rejoin(pin: String)
    case openCoinHistory
    case showNotice(message: String)
}
