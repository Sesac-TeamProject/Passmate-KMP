import Shared

// 코인 사용·충전 내역 (M-12) — Compose CoinHistoryUiState.kt 미러
struct CoinHistoryUiState {
    var isLoading: Bool = true

    var isLoadingMore: Bool = false

    var items: [CoinTransaction] = []

    var hasNext: Bool = false

    var nextCursor: String? = nil

    var hasError: Bool = false

    var isEmpty: Bool {
        !isLoading && !hasError && items.isEmpty
    }
}
