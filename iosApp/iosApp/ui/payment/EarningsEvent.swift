enum EarningsEvent {
    // 정산은 호스트(회원) 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    case requireSignIn
    case openAccountSheet
    case showNotice(message: String)
}
