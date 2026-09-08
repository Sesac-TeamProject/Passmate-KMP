enum WaitingAction {
    case enter(pin: String)
    // 참가자 조회 실패·연결 끊김 후 다시 불러오기
    case retryParticipants
    case clickLeave
    // M-07 "지금 다시 연결" — 백오프 대기를 건너뛰고 즉시 재구독한다
    case reconnect
}
