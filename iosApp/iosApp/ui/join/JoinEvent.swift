enum JoinEvent {
    case requestQrScan
    case joinCompleted(pin: String)
    case paymentRequired(pin: String)
    case signInRequested
    case showNotice(message: String)
}
