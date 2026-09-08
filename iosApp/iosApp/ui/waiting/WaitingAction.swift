enum WaitingAction {
    case enter(pin: String)
    // 참가자 조회 실패·연결 끊김 후 다시 불러오기
    case retryParticipants
    case clickLeave
}
