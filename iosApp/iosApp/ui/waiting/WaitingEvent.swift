enum WaitingEvent {
    case sessionStarted(pin: String)
    case roomClosed(message: String)
    case left
    case showNotice(message: String)
}
