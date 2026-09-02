import Shared

// 코인 내역 (M-12-9) — Compose CoinHistoryUiState.kt 미러
// balance는 조회 실패 시 nil로 두고 카드에서 "-"로 그린다 (목록 실패와 분리)
struct CoinHistoryUiState {
    var isLoading: Bool = true

    var isLoadingMore: Bool = false

    var balance: Int? = nil

    var filter: CoinHistoryFilter = .all

    var items: [CoinTransaction] = []

    var hasNext: Bool = false

    var nextCursor: String? = nil

    var hasError: Bool = false

    // 필터는 화면 표시 전용 — 서버가 준 페이지는 그대로 두고 보이는 목록만 좁힌다
    var visibleItems: [CoinTransaction] {
        items.filter { filter.matches($0) }
    }

    var isEmpty: Bool {
        !isLoading && !hasError && visibleItems.isEmpty
    }
}

// M-12-9 필터 칩 — 부호 기준(충전 = 충전·환급·보너스 등 입금, 사용 = 차감)
enum CoinHistoryFilter: CaseIterable {
    case all
    case charge
    case spend

    var label: String {
        switch self {
        case .all:
            return "전체"
        case .charge:
            return "충전"
        case .spend:
            return "사용"
        }
    }

    func matches(_ transaction: CoinTransaction) -> Bool {
        switch self {
        case .all:
            return true
        case .charge:
            return Int(transaction.amount) >= 0
        case .spend:
            return Int(transaction.amount) < 0
        }
    }
}
