enum WaitingAction {
    case enter(pin: String)
    case clickLeave
    // M-07 "지금 다시 연결" — 백오프 대기를 건너뛰고 즉시 재구독한다
    case reconnect
}
