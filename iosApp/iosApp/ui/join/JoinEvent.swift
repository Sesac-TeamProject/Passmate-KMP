enum JoinEvent {
    case requestQrScan
    case joinCompleted(pin: String)
    case signInRequested
    case showNotice(message: String)
}
