enum JoinEvent {
    case requestQrScan
    case joinCompleted(pin: String)
    case paymentRequired(pin: String)
    case signInRequested
    // 유료 방 게스트 차단·서버 LoginRequired — 로그인 후 결제 화면으로 복귀한다 (스펙 §3)
    case signInRequiredForPaidRoom(pin: String)
    case showNotice(message: String)
}
