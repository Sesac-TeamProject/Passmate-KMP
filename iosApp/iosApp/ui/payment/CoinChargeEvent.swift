enum CoinChargeEvent {
    // 완료 화면에서 "확인" — 마이로 돌아간다
    case done
    case showNotice(message: String)
}
