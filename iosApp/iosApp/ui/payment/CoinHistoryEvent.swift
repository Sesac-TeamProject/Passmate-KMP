// Compose CoinHistoryEvent.kt 미러
enum CoinHistoryEvent {
    case showNotice(message: String)
    // 빈 상태 CTA — 코인 충전 화면으로 이동
    case openCoinCharge
}
