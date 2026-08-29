import Combine
import Foundation
import Shared

// Compose HostedRoomsViewModel.kt 미러 — 내가 만든 방 목록·명성 카드 로드 (M-13)
final class HostedRoomsViewModel: ObservableObject {
    private let getHostedRoomsUseCase: GetHostedRoomsUseCase

    private let getMyGradeUseCase: GetMyGradeUseCase

    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = HostedRoomsUiState()

    let event = PassthroughSubject<HostedRoomsEvent, Never>()

    private var hasEntered = false

    private func onEnter() {
        if hasEntered {
            return
        }
        hasEntered = true
        // 회원 전용 가드 — 서버 검증이 최종 권위지만 UX상 진입 시 먼저 로그인 유도 (규칙 §8)
        if isSignedInUseCase.invoke() {
            load()
        } else {
            event.send(.requireSignIn)
        }
    }

    private func load() {
        uiState.isLoading = true
        uiState.loadFailed = false
        getMyGradeUseCase.invoke { [weak self] gradeResult, _ in
            DispatchQueue.main.async {
                guard let self else { return }
                // 명성 카드는 부가 정보 — 등급 로드 실패는 목록 표시를 막지 않는다
                let grade = (gradeResult as? AppResultSuccess<AnyObject>)?.value as? MyGrade

                self.loadRooms(grade: grade)
            }
        }
    }

    private func loadRooms(grade: MyGrade?) {
        getHostedRoomsUseCase.invoke(cursor: nil) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let page = (result as? AppResultSuccess<AnyObject>)?.value as? PagedResult

                if error == nil, let page {
                    let rooms = self.hostedRooms(page)

                    self.uiState.isLoading = false
                    self.uiState.loadFailed = false
                    self.uiState.grade = grade
                    self.uiState.ongoing = rooms.filter { $0.isOngoing }
                    self.uiState.ended = rooms.filter { !$0.isOngoing }
                    self.uiState.nextCursor = page.hasNext ? page.nextCursor : nil
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
        getHostedRoomsUseCase.invoke(cursor: cursor) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isLoadingMore = false
                if error == nil, let page = (result as? AppResultSuccess<AnyObject>)?.value as? PagedResult {
                    let rooms = self.hostedRooms(page)

                    self.uiState.ongoing += rooms.filter { $0.isOngoing }
                    self.uiState.ended += rooms.filter { !$0.isOngoing }
                    self.uiState.nextCursor = page.hasNext ? page.nextCursor : nil
                } else {
                    self.event.send(.showNotice(message: "목록을 더 불러오지 못했어요"))
                }
            }
        }
    }

    private func onRoomCreated(pin: String) {
        load()
        event.send(.showNotice(message: "방이 만들어졌어요 · PIN \(formatPin(pin))"))
    }

    private func hostedRooms(_ page: PagedResult) -> [HostedRoom] {
        page.items.compactMap { $0 as? HostedRoom }
    }

    private func formatPin(_ pin: String) -> String {
        stride(from: 0, to: pin.count, by: 3).map { start in
            let begin = pin.index(pin.startIndex, offsetBy: start)
            let end = pin.index(begin, offsetBy: min(3, pin.count - start))
            return String(pin[begin..<end])
        }.joined(separator: " ")
    }

    func action(_ action: HostedRoomsAction) {
        switch action {
        case .enter:
            onEnter()
        case .retry:
            load()
        case .loadMore:
            onLoadMore()
        case .clickCreate:
            event.send(.openCreateSheet)
        case .clickReputation:
            event.send(.openReputation)
        case let .clickOngoingRoom(roomId, pin):
            event.send(.openSessionControl(roomId: roomId, pin: pin))
        case let .clickEndedRoom(roomId):
            event.send(.openRoomReport(roomId: roomId))
        case let .roomCreated(pin):
            onRoomCreated(pin: pin)
        case let .notice(message):
            event.send(.showNotice(message: message))
        }
    }

    init(
        getHostedRoomsUseCase: GetHostedRoomsUseCase,
        getMyGradeUseCase: GetMyGradeUseCase,
        isSignedInUseCase: IsSignedInUseCase
    ) {
        self.getHostedRoomsUseCase = getHostedRoomsUseCase
        self.getMyGradeUseCase = getMyGradeUseCase
        self.isSignedInUseCase = isSignedInUseCase
    }
}
