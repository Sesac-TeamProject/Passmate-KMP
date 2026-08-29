import Shared

struct EarningsUiState {
    var isLoading: Bool = true

    var loadFailed: Bool = false

    // 요약·계좌 정보 — 목록은 페이징 append 때문에 items로 분리 관리한다
    var earnings: Earnings?

    var items: [SettlementItem] = []

    var nextCursor: String?

    var isLoadingMore: Bool = false
}
