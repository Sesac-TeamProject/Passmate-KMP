// Compose CoinHistoryAction.kt 미러
enum CoinHistoryAction {
    case enter
    case retry
    case loadMore
    // M-12-9 필터 칩 (전체·충전·사용)
    case selectFilter(filter: CoinHistoryFilter)
    // 빈 상태 CTA "코인 충전하기"
    case clickCharge
}
