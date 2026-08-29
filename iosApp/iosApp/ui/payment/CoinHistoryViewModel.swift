import Combine
import Foundation
import Shared

// Compose CoinHistoryViewModel.kt 미러 — 코인 내역 페이징 로드
final class CoinHistoryViewModel: ObservableObject {
    private let getCoinTransactionsUseCase: GetCoinTransactionsUseCase

    @Published private(set) var uiState = CoinHistoryUiState()

    let event = PassthroughSubject<CoinHistoryEvent, Never>()

    private var isEntered = false

    private func onEnter() {
        if isEntered {
            return
        }
        isEntered = true
        load()
    }

    private func load() {
        uiState.isLoading = true
        uiState.hasError = false
        getCoinTransactionsUseCase.invoke(cursor: nil) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isLoading = false
                if let page = (result as? AppResultSuccess<AnyObject>)?.value as? PagedResult<AnyObject> {
                    self.uiState.hasError = false
                    self.uiState.items = self.transactions(page)
                    self.uiState.hasNext = page.hasNext
                    self.uiState.nextCursor = page.nextCursor
                } else {
                    self.uiState.hasError = true
                }
            }
        }
    }

    private func onLoadMore() {
        if uiState.isLoading || uiState.isLoadingMore || !uiState.hasNext {
            return
        }
        uiState.isLoadingMore = true
        getCoinTransactionsUseCase.invoke(cursor: uiState.nextCursor) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isLoadingMore = false
                if let page = (result as? AppResultSuccess<AnyObject>)?.value as? PagedResult<AnyObject> {
                    self.uiState.items += self.transactions(page)
                    self.uiState.hasNext = page.hasNext
                    self.uiState.nextCursor = page.nextCursor
                } else {
                    self.event.send(.showNotice(message: "내역을 더 불러오지 못했어요"))
                }
            }
        }
    }

    private func transactions(_ page: PagedResult<AnyObject>) -> [CoinTransaction] {
        page.items.compactMap { $0 as? CoinTransaction }
    }

    func action(_ action: CoinHistoryAction) {
        switch action {
        case .enter:
            onEnter()
        case .retry:
            load()
        case .loadMore:
            onLoadMore()
        }
    }

    init(getCoinTransactionsUseCase: GetCoinTransactionsUseCase) {
        self.getCoinTransactionsUseCase = getCoinTransactionsUseCase
    }
}
