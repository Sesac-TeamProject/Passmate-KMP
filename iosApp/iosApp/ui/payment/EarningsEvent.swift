enum EarningsEvent {
    // 정산은 호스트(회원) 전용 — 게스트 진입 시 로그인 유도 (규칙 §8)
    case requireSignIn
    case openAccountSheet
    // 결제·정산 내역 전체 목록 — 기존 코인·결제 내역 화면(M-12)으로 보낸다
    case openCoinHistory
    case showNotice(message: String)
}
