import Combine
import Foundation
import Shared

// Compose RoomListViewModel.kt 미러 — 공개 방 목록 로드·검색·필터·페이징
final class RoomListViewModel: ObservableObject {
    private let getPublicRoomsUseCase: GetPublicRoomsUseCase

    @Published private(set) var uiState = RoomListUiState()

    let event = PassthroughSubject<RoomListEvent, Never>()

    private func onChangeQuery(query: String) {
        uiState.query = query
    }

    private func onSelectType(type: RoomTypeFilter) {
        if uiState.typeFilter != type {
            uiState.typeFilter = type
            reload()
        }
    }

    private func reload() {
        uiState.isLoading = true
        uiState.hasError = false

        let query = trimmedQuery()

        getPublicRoomsUseCase.invoke(
            sort: RoomSort.popular,
            query: query,
            type: uiState.typeFilter,
            cursor: nil
        ) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isLoading = false
                if let page = (result as? AppResultSuccess<AnyObject>)?.value as? PagedResult {
                    self.uiState.hasError = false
                    self.uiState.rooms = self.publicRooms(page)
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
        getPublicRoomsUseCase.invoke(
            sort: RoomSort.popular,
            query: trimmedQuery(),
            type: uiState.typeFilter,
            cursor: uiState.nextCursor
        ) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isLoadingMore = false
                if let page = (result as? AppResultSuccess<AnyObject>)?.value as? PagedResult {
                    self.uiState.rooms += self.publicRooms(page)
                    self.uiState.hasNext = page.hasNext
                    self.uiState.nextCursor = page.nextCursor
                } else {
                    self.event.send(.showNotice(message: "목록을 더 불러오지 못했어요"))
                }
            }
        }
    }

    private func publicRooms(_ page: PagedResult) -> [PublicRoom] {
        page.items.compactMap { $0 as? PublicRoom }
    }

    private func trimmedQuery() -> String? {
        let trimmed = uiState.query.trimmingCharacters(in: .whitespaces)

        return trimmed.isEmpty ? nil : trimmed
    }

    func action(_ action: RoomListAction) {
        switch action {
        case let .changeQuery(query):
            onChangeQuery(query: query)
        case .submitSearch:
            reload()
        case let .selectType(type):
            onSelectType(type: type)
        case let .clickRoom(pin):
            event.send(.openRoom(pin: pin))
        case let .clickHost(hostId):
            event.send(.openHostProfile(hostId: hostId))
        case .loadMore:
            onLoadMore()
        case .retry:
            reload()
        case .clickPinEntry:
            event.send(.openPinEntry)
        case let .notice(message):
            event.send(.showNotice(message: message))
        }
    }

    init(getPublicRoomsUseCase: GetPublicRoomsUseCase) {
        self.getPublicRoomsUseCase = getPublicRoomsUseCase
        reload()
    }
}
