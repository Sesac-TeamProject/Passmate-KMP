import Combine
import Foundation
import Shared

// Compose EarningsViewModel.kt 미러 — 수익·정산 요약·내역 로드 (M-T4)
final class EarningsViewModel: ObservableObject {
    private let getEarningsUseCase: GetEarningsUseCase

    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = EarningsUiState()

    let event = PassthroughSubject<EarningsEvent, Never>()

    private var hasEntered = false

    private func onEnter() {
        if hasEntered {
            return
        }
        hasEntered = true
        // 호스트(회원) 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if isSignedInUseCase.invoke() {
            load()
        } else {
            event.send(.requireSignIn)
        }
    }

    private func load() {
        uiState.isLoading = true
        uiState.loadFailed = false
        getEarningsUseCase.invoke(cursor: nil) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let earnings = (result as? AppResultSuccess<AnyObject>)?.value as? Earnings

                if error == nil, let earnings {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = false
                    self.uiState.earnings = earnings
                    self.uiState.items = self.settlementItems(earnings)
                    self.uiState.nextCursor = earnings.hasNext ? earnings.nextCursor : nil
                } else {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = true
                }
            }
        }
    }

    private func onLoadMore() {
        guard let cursor = uiState.nextCursor, !uiState.isLoadingMore else { return }
        uiState.isLoadingMore = true
        getEarningsUseCase.invoke(cursor: cursor) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isLoadingMore = false
                if error == nil, let earnings = (result as? AppResultSuccess<AnyObject>)?.value as? Earnings {
                    self.uiState.items += self.settlementItems(earnings)
                    self.uiState.nextCursor = earnings.hasNext ? earnings.nextCursor : nil
                } else {
                    self.event.send(.showNotice(message: "내역을 더 불러오지 못했어요"))
                }
            }
        }
    }

    private func onAccountSaved() {
        load()
        event.send(.showNotice(message: "정산 계좌를 저장했어요"))
    }

    private func settlementItems(_ earnings: Earnings) -> [SettlementItem] {
        earnings.items.compactMap { $0 as? SettlementItem }
    }

    func action(_ action: EarningsAction) {
        switch action {
        case .enter:
            onEnter()
        case .retry:
            load()
        case .loadMore:
            onLoadMore()
        case .clickViewAllHistory:
            event.send(.openCoinHistory)
        case .clickManageAccount:
            event.send(.openAccountSheet)
        case .accountSaved:
            onAccountSaved()
        case let .notice(message):
            event.send(.showNotice(message: message))
        }
    }

    init(
        getEarningsUseCase: GetEarningsUseCase,
        isSignedInUseCase: IsSignedInUseCase
    ) {
        self.getEarningsUseCase = getEarningsUseCase
        self.isSignedInUseCase = isSignedInUseCase
    }
}
