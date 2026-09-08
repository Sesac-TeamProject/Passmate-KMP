enum WaitingEvent {
    case sessionStarted(pin: String)
    // 세션이 끝났다 — 대기실을 스택에서 걷어내고 결과로 보낸다 (규칙 §2-1-2)
    case sessionFinished(roomId: Int64)
    case roomClosed(message: String)
    case left
    case showNotice(message: String)
}
