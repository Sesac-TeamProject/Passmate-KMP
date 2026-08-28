enum PaymentEvent {
    case enterRoom(pin: String)
    case showNotice(message: String)
    case signInRequired
}
