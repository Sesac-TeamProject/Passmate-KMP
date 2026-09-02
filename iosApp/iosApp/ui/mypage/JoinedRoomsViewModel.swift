import Combine
import Foundation
import Shared

final class JoinedRoomsViewModel: ObservableObject {
    // 문의 창구(고객센터·메일)는 아직 계약·라우트에 없다 — 연결 전까지 안내 문구만 노출한다
    private static let contactSupportNotice = "문의 창구는 준비 중이에요"

    private let getMyPageUseCase: GetMyPageUseCase

    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState: JoinedRoomsUiState

    let event = PassthroughSubject<JoinedRoomsEvent, Never>()

    private var hasEntered = false

    private func onEnter() {
        if hasEntered {
            return
        }
        hasEntered = true
        // 회원 전용 가드 — 서버 검증이 최종 권위지만 UX상 진입 시 먼저 로그인 유도 (규칙 §8)
        if isSignedInUseCase.invoke() {
            loadFirstPage()
        } else {
            event.send(.requireSignIn)
        }
    }

    private func loadFirstPage() {
        uiState.isLoading = true
        uiState.loadFailed = false
        getMyPageUseCase.invoke(cursor: nil) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let success = result as? AppResultSuccess<AnyObject>

                if error == nil, let myPage = success?.value as? MyPage {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = false
                    self.uiState.summary = myPage.summary
                    self.uiState.ongoing = myPage.ongoing
                    self.uiState.rooms = myPage.rooms
                    self.uiState.nextCursor = myPage.nextCursor
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
        getMyPageUseCase.invoke(cursor: cursor) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let success = result as? AppResultSuccess<AnyObject>

                if error == nil, let myPage = success?.value as? MyPage {
                    self.uiState.isLoadingMore = false
                    self.uiState.rooms = self.uiState.rooms + myPage.rooms
                    self.uiState.nextCursor = myPage.nextCursor
                } else {
                    self.uiState.isLoadingMore = false
                    self.event.send(.showNotice(message: "목록을 더 불러오지 못했어요"))
                }
            }
        }
    }

    func action(_ action: JoinedRoomsAction) {
        switch action {
        case .enter:
            onEnter()
        case .retry:
            loadFirstPage()
        case .loadMore:
            onLoadMore()
        case let .clickRoomReport(roomId):
            event.send(.openReport(roomId: roomId))
        case let .clickRejoin(pin):
            event.send(.rejoin(pin: pin))
        case .clickEnterPin:
            event.send(.openPinEntry)
        case .clickContactSupport:
            event.send(.showNotice(message: Self.contactSupportNotice))
        }
    }

    init(
        getMyPageUseCase: GetMyPageUseCase,
        isSignedInUseCase: IsSignedInUseCase
    ) {
        self.getMyPageUseCase = getMyPageUseCase
        self.isSignedInUseCase = isSignedInUseCase
        self.uiState = JoinedRoomsUiState()
    }
}
