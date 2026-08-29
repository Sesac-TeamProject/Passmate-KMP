enum EarningsAction {
    case enter
    case retry
    case loadMore
    // 계좌 관리 → 정산 계좌 시트 (M-12-3, 시트 표시는 화면이 소유)
    case clickManageAccount
    // 시트에서 계좌 저장 완료 — 하단 계좌 요약 갱신
    case accountSaved
    case notice(message: String)
}
