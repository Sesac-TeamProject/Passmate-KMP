import Combine
import Foundation
import Shared

// Compose CoinHistoryViewModel.kt 미러 — 보유 코인 + 코인 내역 페이징 로드
final class CoinHistoryViewModel: ObservableObject {
    private let getMyCoinsUseCase: GetMyCoinsUseCase

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

    // 보유 코인은 실패해도 화면을 덮지 않는다 — 실패 화면은 "목록" 로드 실패일 때만이다 (E-List)
    private func loadBalance() {
        getMyCoinsUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                if let coins = (result as? AppResultSuccess<AnyObject>)?.value as? CoinBalance {
                    self.uiState.balance = Int(coins.balance)
                } else {
                    self.uiState.balance = nil
                }
            }
        }
    }

    private func loadTransactions() {
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

    private func load() {
        uiState.isLoading = true
        uiState.hasError = false
        loadBalance()
        loadTransactions()
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

    private func onSelectFilter(_ filter: CoinHistoryFilter) {
        uiState.filter = filter
    }

    private func onClickCharge() {
        event.send(.openCoinCharge)
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
        case let .selectFilter(filter):
            onSelectFilter(filter)
        case .clickCharge:
            onClickCharge()
        }
    }

    init(getMyCoinsUseCase: GetMyCoinsUseCase, getCoinTransactionsUseCase: GetCoinTransactionsUseCase) {
        self.getMyCoinsUseCase = getMyCoinsUseCase
        self.getCoinTransactionsUseCase = getCoinTransactionsUseCase
    }
}
