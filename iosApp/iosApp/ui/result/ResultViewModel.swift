import Combine
import Foundation
import Shared

final class ResultViewModel: ObservableObject {
    // Compose ResultViewModel.CONTACT_NOTICE와 동일 문구 (규칙 §14 미러 1:1)
    private static let contactNotice = "문의 접수는 준비 중이에요. 잠시 후 다시 시도해 주세요"

    private let getSessionResultUseCase: GetSessionResultUseCase

    private let getLearningReportUseCase: GetLearningReportUseCase

    private let buildReportSummaryUseCase: BuildReportSummaryUseCase

    private let getMyParticipationUseCase: GetMyParticipationUseCase

    private let requestGuestClaimUseCase: RequestGuestClaimUseCase

    private let submitRatingUseCase: SubmitRatingUseCase

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
                        let shouldPromptRating = sessionResult.canRate
                            && !self.uiState.hasRated
                            && !self.uiState.hasPromptedRating

                        if shouldPromptRating {
                            self.uiState.isRatingSheetVisible = true
                            self.uiState.hasPromptedRating = true
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

    // 문의 채널이 계약(contracts/)·라우트에 아직 없다 — 안내 문구만 노출하고 채널이 정해지면 교체한다
    private func onClickContactSupport() {
        event.send(.showNotice(message: Self.contactNotice))
    }

    // 게스트 가입 유도 — participantId를 대기 큐에 넣고 로그인 화면으로 (FR-036)
    private func onClickSignup() {
        if let participation = getMyParticipationUseCase.invoke() {
            requestGuestClaimUseCase.invoke(participantId: participation.participantId)
        }
        event.send(.navigateToSignup)
    }

    // ─── 평가 시트 (T080) ───

    private func onToggleRatingTag(_ tag: RatingTag) {
        if uiState.ratingTags.contains(tag) {
            uiState.ratingTags.remove(tag)
        } else {
            uiState.ratingTags.insert(tag)
        }
    }

    // 최종 중복 차단은 서버(409 ALREADY_RATED) — 클라 in-flight 가드는 UX용 (규칙 §9)
    private func onSubmitRating() {
        guard let roomId, !uiState.isSubmittingRating else { return }
        if uiState.ratingStars < 1 {
            event.send(.showNotice(message: "별점을 선택해 주세요"))
            return
        }
        uiState.isSubmittingRating = true
        let draft = RatingDraft(
            stars: Int32(uiState.ratingStars),
            tags: Set(uiState.ratingTags),
            comment: uiState.ratingComment
        )

        submitRatingUseCase.invoke(roomId: roomId, draft: draft) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isSubmittingRating = false
                if error == nil, result is AppResultSuccess<AnyObject> {
                    self.uiState.isRatingSheetVisible = false
                    self.uiState.hasRated = true
                    self.event.send(.ratingSubmitted(message: "평가를 보냈어요. 고마워요!"))
                } else {
                    self.handleRatingFailure((result as? AppResultFailure)?.error)
                }
            }
        }
    }

    private func handleRatingFailure(_ error: AppError?) {
        if error is AppError.Conflict {
            uiState.isRatingSheetVisible = false
            uiState.hasRated = true
            event.send(.showNotice(message: "이미 평가한 세션이에요"))
        } else if error is AppError.Gone {
            uiState.isRatingSheetVisible = false
            event.send(.showNotice(message: "평가 기간(24시간)이 지났어요"))
        } else if error is AppError.NetworkError {
            event.send(.showNotice(message: "네트워크 연결을 확인해 주세요"))
        } else {
            event.send(.showNotice(message: "평가를 보내지 못했어요. 다시 시도해 주세요"))
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
        case .clickSignup:
            onClickSignup()
        case .retry:
            onRetry()
        case .clickContactSupport:
            onClickContactSupport()
        case .openRatingSheet:
            uiState.isRatingSheetVisible = true
        case .dismissRatingSheet:
            uiState.isRatingSheetVisible = false
        case let .selectRatingStars(stars):
            uiState.ratingStars = stars
        case let .toggleRatingTag(tag):
            onToggleRatingTag(tag)
        case let .changeRatingComment(comment):
            uiState.ratingComment = String(comment.prefix(100))
        case .submitRating:
            onSubmitRating()
        case .skipRating:
            uiState.isRatingSheetVisible = false
        }
    }

    func stopWatching() {
        eventWatcher.stop()
    }

    init(
        getSessionResultUseCase: GetSessionResultUseCase,
        getLearningReportUseCase: GetLearningReportUseCase,
        buildReportSummaryUseCase: BuildReportSummaryUseCase,
        getMyParticipationUseCase: GetMyParticipationUseCase,
        requestGuestClaimUseCase: RequestGuestClaimUseCase,
        submitRatingUseCase: SubmitRatingUseCase,
        eventWatcher: SessionEventStreamWatcher
    ) {
        self.getSessionResultUseCase = getSessionResultUseCase
        self.getLearningReportUseCase = getLearningReportUseCase
        self.buildReportSummaryUseCase = buildReportSummaryUseCase
        self.getMyParticipationUseCase = getMyParticipationUseCase
        self.requestGuestClaimUseCase = requestGuestClaimUseCase
        self.submitRatingUseCase = submitRatingUseCase
        self.eventWatcher = eventWatcher
        self.uiState = ResultUiState()
    }

    deinit {
        eventWatcher.stop()
    }
}
