import Combine
import Foundation
import Shared

// Compose RoomReportViewModel.kt 미러 — 방 리포트 로드·탭·내보내기 (M-14)
final class RoomReportViewModel: ObservableObject {
    private let getRoomReportUseCase: GetRoomReportUseCase

    private let buildRoomReportSummaryUseCase: BuildRoomReportSummaryUseCase

    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = RoomReportUiState()

    let event = PassthroughSubject<RoomReportEvent, Never>()

    private var hasEntered = false

    private func onEnter(roomId: Int64) {
        if hasEntered {
            return
        }
        hasEntered = true
        // 호스트(회원) 전용 가드 — 서버 403이 최종 권위지만 UX상 진입 시 먼저 로그인 유도 (규칙 §8)
        if isSignedInUseCase.invoke() {
            load(roomId: roomId)
        } else {
            event.send(.requireSignIn)
        }
    }

    private func load(roomId: Int64) {
        uiState.isLoading = true
        uiState.loadFailed = false
        getRoomReportUseCase.invoke(roomId: roomId) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let report = (result as? AppResultSuccess<AnyObject>)?.value as? RoomReport

                if error == nil, let report {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = false
                    self.uiState.report = report
                } else {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = true
                }
            }
        }
    }

    private func onClickExport() {
        if let report = uiState.report {
            let summary = buildRoomReportSummaryUseCase.invoke(report: report)

            event.send(.shareReport(summary: summary))
        }
    }

    func action(_ action: RoomReportAction) {
        switch action {
        case let .enter(roomId):
            onEnter(roomId: roomId)
        case let .retry(roomId):
            load(roomId: roomId)
        case let .selectTab(tab):
            uiState.selectedTab = tab
        case let .selectStudentSort(sort):
            uiState.studentSort = sort
        case .clickExport:
            onClickExport()
        }
    }

    init(
        getRoomReportUseCase: GetRoomReportUseCase,
        buildRoomReportSummaryUseCase: BuildRoomReportSummaryUseCase,
        isSignedInUseCase: IsSignedInUseCase
    ) {
        self.getRoomReportUseCase = getRoomReportUseCase
        self.buildRoomReportSummaryUseCase = buildRoomReportSummaryUseCase
        self.isSignedInUseCase = isSignedInUseCase
    }
}
