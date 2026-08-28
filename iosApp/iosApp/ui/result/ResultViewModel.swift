import Combine
import Foundation
import Shared

final class ResultViewModel: ObservableObject {
    private let getSessionResultUseCase: GetSessionResultUseCase

    private let getLearningReportUseCase: GetLearningReportUseCase

    private let buildReportSummaryUseCase: BuildReportSummaryUseCase

    private let eventWatcher: SessionEventStreamWatcher

    @Published private(set) var uiState: ResultUiState

    let event = PassthroughSubject<ResultEvent, Never>()

    private var roomId: Int64?

    private func onEnter(roomId: Int64) {
        if self.roomId != nil {
            return
        }
        self.roomId = roomId
        load(roomId: roomId)
        observeUpdates(roomId: roomId)
    }

    private func load(roomId: Int64) {
        uiState.isLoading = true
        uiState.loadFailed = false
        getLearningReportUseCase.invoke(roomId: roomId) { [weak self] reportResult, _ in
            let report = (reportResult as? AppResultSuccess<AnyObject>)?.value as? LearningReport

            self?.getSessionResultUseCase.invoke(roomId: roomId) { [weak self] result, error in
                DispatchQueue.main.async {
                    guard let self else { return }
                    let success = result as? AppResultSuccess<AnyObject>

                    if error == nil, let sessionResult = success?.value as? SessionResult {
                        self.uiState.isLoading = false
                        self.uiState.loadFailed = false
                        self.uiState.result = sessionResult
                        self.uiState.report = report
                        if self.uiState.selectedQuestionNo == nil {
                            self.uiState.selectedQuestionNo = self.firstAiQuestionNo(sessionResult)
                        }
                    } else {
                        self.uiState.isLoading = false
                        self.uiState.loadFailed = true
                    }
                }
            }
        }
    }

    private func firstAiQuestionNo(_ result: SessionResult) -> Int? {
        if let aiQuestion = result.questions.first(where: { $0.aiFeedback != nil }) {
            return Int(aiQuestion.questionNo)
        } else if let first = result.questions.first {
            return Int(first.questionNo)
        } else {
            return nil
        }
    }

    // AI 분석 완료·첨삭 도착·리포트 생성 시 결과를 다시 불러온다 (FR-027·035, SC-009)
    private func observeUpdates(roomId: Int64) {
        eventWatcher.start(roomId: roomId) { [weak self] streamEvent in
            guard let self else { return }
            if let received = streamEvent as? SessionEventStreamStreamEventReceived {
                let serverEvent = received.frame.event

                if serverEvent is ServerEventFeedbackReady
                    || serverEvent is ServerEventFeedbackFailed
                    || serverEvent is ServerEventReviewReceived
                    || serverEvent is ServerEventReportReady {
                    self.reload(roomId: roomId)
                }
            }
        }
    }

    private func reload(roomId: Int64) {
        getLearningReportUseCase.invoke(roomId: roomId) { [weak self] reportResult, _ in
            let report = (reportResult as? AppResultSuccess<AnyObject>)?.value as? LearningReport

            self?.getSessionResultUseCase.invoke(roomId: roomId) { [weak self] result, error in
                DispatchQueue.main.async {
                    guard let self else { return }
                    let success = result as? AppResultSuccess<AnyObject>

                    if error == nil, let sessionResult = success?.value as? SessionResult {
                        self.uiState.result = sessionResult
                        if let report {
                            self.uiState.report = report
                        }
                    }
                }
            }
        }
    }

    private func onSelectQuestion(questionNo: Int) {
        if uiState.selectedQuestionNo == questionNo {
            uiState.selectedQuestionNo = nil
        } else {
            uiState.selectedQuestionNo = questionNo
        }
    }

    private func onClickExport() {
        guard let result = uiState.result, !uiState.isSharing else { return }
        let summary = buildReportSummaryUseCase.invoke(result: result, report: uiState.report)

        event.send(.shareReport(summary: summary))
    }

    private func onRetry() {
        if let roomId {
            load(roomId: roomId)
        }
    }

    func action(_ action: ResultAction) {
        switch action {
        case let .enter(roomId):
            onEnter(roomId: roomId)
        case let .selectQuestion(questionNo):
            onSelectQuestion(questionNo: questionNo)
        case .clickExport:
            onClickExport()
        case .retry:
            onRetry()
        }
    }

    func stopWatching() {
        eventWatcher.stop()
    }

    init(
        getSessionResultUseCase: GetSessionResultUseCase,
        getLearningReportUseCase: GetLearningReportUseCase,
        buildReportSummaryUseCase: BuildReportSummaryUseCase,
        eventWatcher: SessionEventStreamWatcher
    ) {
        self.getSessionResultUseCase = getSessionResultUseCase
        self.getLearningReportUseCase = getLearningReportUseCase
        self.buildReportSummaryUseCase = buildReportSummaryUseCase
        self.eventWatcher = eventWatcher
        self.uiState = ResultUiState()
    }

    deinit {
        eventWatcher.stop()
    }
}
