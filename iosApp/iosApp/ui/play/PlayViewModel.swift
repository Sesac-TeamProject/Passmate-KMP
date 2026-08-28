import Combine
import Foundation
import Shared

final class PlayViewModel: ObservableObject {
    private let getRoomInfoUseCase: GetRoomInfoUseCase

    private let getSessionSnapshotUseCase: GetSessionSnapshotUseCase

    private let submitAnswerUseCase: SubmitAnswerUseCase

    private let getVoiceHintsUseCase: GetVoiceHintsUseCase

    private let leaveRoomUseCase: LeaveRoomUseCase

    private let getMyParticipationUseCase: GetMyParticipationUseCase

    private let snapshotPolicy: SnapshotPolicy

    private let eventWatcher: SessionEventStreamWatcher

    @Published private(set) var uiState: PlayUiState

    let event = PassthroughSubject<PlayEvent, Never>()

    private var roomId: Int64?

    private var snapshotTs: String?

    private var deadline: QuestionDeadline?

    private var ticker: Timer?

    private func onEnter(pin: String) {
        if roomId != nil {
            return
        }
        let my = getMyParticipationUseCase.invoke()

        uiState.myParticipantId = my?.participantId
        uiState.myNickname = my?.nickname
        getRoomInfoUseCase.invoke(pin: pin) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let success = result as? AppResultSuccess<AnyObject>

                if error == nil, let room = success?.value as? RoomInfo {
                    self.roomId = room.roomId
                    self.observeRoomEvents(roomId: room.roomId)
                } else {
                    self.uiState.isLoading = false
                    self.event.send(.roomClosed(message: self.errorMessage((result as? AppResultFailure)?.error)))
                }
            }
        }
    }

    private func observeRoomEvents(roomId: Int64) {
        eventWatcher.start(roomId: roomId) { [weak self] streamEvent in
            guard let self else { return }
            if streamEvent is SessionEventStreamStreamEventConnected {
                self.loadSnapshot(roomId: roomId)
            } else if let received = streamEvent as? SessionEventStreamStreamEventReceived {
                self.handleFrame(received.frame)
            }
        }
    }

    // 재접속·늦은 입장 복구 — 스냅샷 적용 후 이후 이벤트만 증분 반영 (규칙 §2-1-2)
    private func loadSnapshot(roomId: Int64) {
        getSessionSnapshotUseCase.invoke(roomId: roomId) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let success = result as? AppResultSuccess<AnyObject>

                if error == nil, let snapshot = success?.value as? SessionSnapshot {
                    self.snapshotTs = snapshot.ts
                    self.applySnapshot(snapshot)
                    self.restoreVoiceHint(roomId: roomId, currentQuestionNo: snapshot.currentQuestion?.questionNo)
                } else {
                    self.uiState.isLoading = false
                }
            }
        }
    }

    // 재접속 시 현재 문항의 마지막 힌트를 복구한다 — 자동 재생 없이 다시 듣기만 (FR-041)
    private func restoreVoiceHint(roomId: Int64, currentQuestionNo: Int32?) {
        if let currentQuestionNo {
            getVoiceHintsUseCase.invoke(roomId: roomId) { [weak self] result, error in
                DispatchQueue.main.async {
                    guard let self else { return }
                    let success = result as? AppResultSuccess<AnyObject>

                    if error == nil, let hints = success?.value as? [VoiceHint] {
                        self.uiState.activeVoiceHint = hints.last { $0.questionNo == currentQuestionNo }
                    }
                }
            }
        }
    }

    private func applySnapshot(_ snapshot: SessionSnapshot) {
        let question = snapshot.currentQuestion
        let myAnswer = snapshot.myAnswers.first { $0.questionId == question?.questionId }

        if let question, !question.isClosed {
            deadline = QuestionDeadline.companion.fromServerTimes(endsAt: question.endsAt, serverNow: snapshot.ts)
        } else {
            deadline = nil
        }
        uiState.isLoading = false
        if snapshot.status == RoomStatus.finished {
            uiState.phase = .finished
        } else if question != nil {
            uiState.phase = .question
        } else {
            uiState.phase = .idle
        }
        uiState.questionCount = snapshot.questionCount.map { Int(truncating: $0) } ?? uiState.questionCount
        uiState.question = question
        uiState.selectedChoiceIndex = nil
        uiState.essayAnswer = ""
        uiState.hasSubmitted = myAnswer != nil
        uiState.myAnswerResult = nil
        uiState.reveal = nil
        uiState.totalScore = snapshot.totalScore.map { Double(truncating: $0) } ?? uiState.totalScore
        uiState.myCorrectCount = snapshot.myAnswers.filter { $0.correct?.boolValue == true }.count
        uiState.rank = snapshot.rank.map { Int(truncating: $0) } ?? uiState.rank
        uiState.ranking = snapshot.ranking
        if snapshot.status == RoomStatus.finished {
            uiState.finalRanking = snapshot.ranking
        }
        uiState.isLocked = snapshot.isLocked
        restartTicker()
    }

    private func handleFrame(_ frame: ServerEventFrame) {
        if let snapshotTs, snapshotPolicy.isStaleFrame(frameTs: frame.ts, snapshotTs: snapshotTs) {
            return
        }
        let serverEvent = frame.event

        if let started = serverEvent as? ServerEventQuestionStarted {
            onQuestionStarted(started, serverTs: frame.ts)
        } else if let ended = serverEvent as? ServerEventQuestionEnded {
            onQuestionEnded(ended)
        } else if let ranking = serverEvent as? ServerEventRankingUpdated {
            uiState.ranking = ranking.ranking.map(toRankEntry)
        } else if let locked = serverEvent as? ServerEventScreenLocked {
            uiState.isLocked = locked.locked
        } else if let sessionStarted = serverEvent as? ServerEventSessionStarted {
            uiState.isLoading = false
            uiState.questionCount = Int(sessionStarted.questionCount)
        } else if let sessionEnded = serverEvent as? ServerEventSessionEnded {
            onSessionEnded(sessionEnded)
        } else if let hintPublished = serverEvent as? ServerEventHintPublished {
            onHintPublished(hintPublished)
        } else if serverEvent is ServerEventRoomCancelled {
            event.send(.roomClosed(message: "방이 취소됐어요"))
        } else if let left = serverEvent as? ServerEventParticipantLeft {
            onParticipantLeft(left)
        }
    }

    private func onQuestionStarted(_ started: ServerEventQuestionStarted, serverTs: String) {
        let question = SessionQuestion(
            questionId: started.questionId,
            questionNo: started.questionNo,
            type: QuestionType.companion.from(raw: started.type),
            body: started.body,
            choices: started.choices ?? [],
            points: started.points,
            timeLimitSec: started.timeLimitSec,
            endsAt: started.endsAt,
            isClosed: false
        )

        deadline = QuestionDeadline.companion.fromServerTimes(endsAt: started.endsAt, serverNow: serverTs)
        uiState.isLoading = false
        uiState.phase = .question
        uiState.question = question
        uiState.selectedChoiceIndex = nil
        uiState.essayAnswer = ""
        uiState.remainingSeconds = deadline.map { Int($0.remainingSeconds()) } ?? Int(started.timeLimitSec)
        uiState.isSubmitting = false
        uiState.hasSubmitted = false
        uiState.myAnswerResult = nil
        uiState.reveal = nil
        uiState.activeVoiceHint = nil
        restartTicker()
    }

    private func onQuestionEnded(_ ended: ServerEventQuestionEnded) {
        deadline = nil
        stopTicker()
        uiState.phase = .idle
        uiState.remainingSeconds = 0
        uiState.reveal = PlayUiState.Reveal(
            answer: ended.answerReveal.answer,
            explanation: ended.answerReveal.explanation,
            correctAnswererCount: Int(ended.correctCount)
        )
    }

    private func onSessionEnded(_ ended: ServerEventSessionEnded) {
        let finalRanking = ended.finalRanking.map(toRankEntry)
        let myEntry = finalRanking.first { $0.participantId == uiState.myParticipantId }

        deadline = nil
        stopTicker()
        uiState.isLoading = false
        uiState.phase = .finished
        uiState.finalRanking = finalRanking
        if let myEntry {
            uiState.rank = Int(myEntry.rank)
            uiState.totalScore = myEntry.total
        }
    }

    // 수신 즉시 자동 재생(FR-040, 3초 SLA) — 재생 실패 시 배너의 수동 재생으로 폴백된다
    private func onHintPublished(_ published: ServerEventHintPublished) {
        let hint = VoiceHint(
            hintId: published.hintId,
            questionNo: published.questionNo,
            clipUrl: published.clipUrl,
            durationMs: published.durationMs
        )

        uiState.activeVoiceHint = hint
        event.send(.playVoiceHint(hint: hint))
    }

    private func onParticipantLeft(_ left: ServerEventParticipantLeft) {
        let isMe = left.participantId == uiState.myParticipantId
        let isKicked = left.reason == ServerEventParticipantLeft.companion.REASON_KICKED

        if isMe && isKicked {
            event.send(.roomClosed(message: "선생님이 내보냈어요"))
        }
    }

    private func restartTicker() {
        stopTicker()
        if deadline != nil {
            uiState.remainingSeconds = deadline.map { Int($0.remainingSeconds()) } ?? 0
            ticker = Timer.scheduledTimer(withTimeInterval: 0.2, repeats: true) { [weak self] _ in
                guard let self else { return }
                if let deadline = self.deadline {
                    self.uiState.remainingSeconds = Int(deadline.remainingSeconds())
                } else {
                    self.stopTicker()
                }
            }
        }
    }

    private func stopTicker() {
        ticker?.invalidate()
        ticker = nil
    }

    private func onSelectChoice(index: Int) {
        if !uiState.hasSubmitted && !uiState.isLocked {
            uiState.selectedChoiceIndex = index
        }
    }

    private func onChangeEssayAnswer(text: String) {
        if !uiState.hasSubmitted {
            uiState.essayAnswer = text
        }
    }

    private func buildAnswerContent() -> String? {
        guard let question = uiState.question else { return nil }
        if question.type == QuestionType.ox {
            if uiState.selectedChoiceIndex == 0 {
                return "O"
            } else if uiState.selectedChoiceIndex == 1 {
                return "X"
            } else {
                return nil
            }
        } else if question.type == QuestionType.essay {
            let trimmed = uiState.essayAnswer.trimmingCharacters(in: .whitespacesAndNewlines)

            return trimmed.isEmpty ? nil : trimmed
        } else {
            if let index = uiState.selectedChoiceIndex, index < question.choices.count {
                return question.choices[index]
            } else {
                return nil
            }
        }
    }

    // 클라이언트 가드는 UX 목적 — 마감·중복의 최종 판정은 서버(410·409)가 한다 (규칙 §1)
    private func onClickSubmit() {
        let content = buildAnswerContent()

        if uiState.isSubmitting || uiState.hasSubmitted || roomId == nil || uiState.question == nil {
            return
        }
        if uiState.isLocked {
            event.send(.showNotice(message: "선생님이 화면을 잠갔어요"))
        } else if let content, let roomId, let question = uiState.question {
            uiState.isSubmitting = true
            submitAnswer(roomId: roomId, questionId: question.questionId, content: content)
        } else {
            event.send(.showNotice(message: "답을 선택하거나 입력해 주세요"))
        }
    }

    private func submitAnswer(roomId: Int64, questionId: Int64, content: String) {
        submitAnswerUseCase.invoke(roomId: roomId, questionId: questionId, content: content) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let success = result as? AppResultSuccess<AnyObject>

                if error == nil, let answerResult = success?.value as? AnswerResult {
                    self.applyAnswerResult(answerResult)
                } else {
                    self.uiState.isSubmitting = false
                    self.handleSubmitFailure(error: (result as? AppResultFailure)?.error)
                }
            }
        }
    }

    private func applyAnswerResult(_ result: AnswerResult) {
        uiState.isSubmitting = false
        uiState.hasSubmitted = true
        uiState.myAnswerResult = result
        uiState.totalScore = result.totalScore
        if result.correct?.boolValue == true {
            uiState.myCorrectCount += 1
        }
        if let rank = result.rank {
            uiState.rank = Int(truncating: rank)
        }
    }

    private func handleSubmitFailure(error: AppError?) {
        if error is AppErrorGone {
            uiState.hasSubmitted = true
            event.send(.showNotice(message: "이미 마감된 문항이에요"))
        } else if error is AppErrorConflict {
            uiState.hasSubmitted = true
            event.send(.showNotice(message: "이미 제출한 문항이에요"))
        } else {
            event.send(.showNotice(message: errorMessage(error)))
        }
    }

    private func onConfirmLeave() {
        let leavingRoomId = roomId

        deadline = nil
        stopTicker()
        eventWatcher.stop()
        if let leavingRoomId {
            leaveRoomUseCase.invoke(roomId: leavingRoomId) { [weak self] _, _ in
                DispatchQueue.main.async {
                    self?.event.send(.left)
                }
            }
        } else {
            event.send(.left)
        }
    }

    private func onClickReplayHint() {
        if let hint = uiState.activeVoiceHint {
            event.send(.playVoiceHint(hint: hint))
        }
    }

    private func onClickViewReport() {
        if let roomId {
            event.send(.openResult(roomId: roomId))
        }
    }

    private func toRankEntry(_ entry: ServerEventRankingEntry) -> RankEntry {
        return RankEntry(
            rank: entry.rank,
            participantId: entry.participantId,
            nickname: entry.nickname,
            avatarId: entry.avatarId,
            total: entry.total
        )
    }

    private func errorMessage(_ error: AppError?) -> String {
        if error is AppErrorNotFound {
            return "방을 찾을 수 없어요"
        } else if error is AppErrorGone {
            return "이미 종료된 방이에요"
        } else if error is AppErrorNetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return "요청에 실패했어요. 잠시 후 다시 시도해 주세요"
        }
    }

    func action(_ action: PlayAction) {
        switch action {
        case let .enter(pin):
            onEnter(pin: pin)
        case let .selectChoice(index):
            onSelectChoice(index: index)
        case let .changeEssayAnswer(text):
            onChangeEssayAnswer(text: text)
        case .clickSubmit:
            onClickSubmit()
        case .clickReplayHint:
            onClickReplayHint()
        case .confirmLeave:
            onConfirmLeave()
        case .clickViewReport:
            onClickViewReport()
        }
    }

    func stopWatching() {
        stopTicker()
        eventWatcher.stop()
    }

    init(
        getRoomInfoUseCase: GetRoomInfoUseCase,
        getSessionSnapshotUseCase: GetSessionSnapshotUseCase,
        submitAnswerUseCase: SubmitAnswerUseCase,
        getVoiceHintsUseCase: GetVoiceHintsUseCase,
        leaveRoomUseCase: LeaveRoomUseCase,
        getMyParticipationUseCase: GetMyParticipationUseCase,
        snapshotPolicy: SnapshotPolicy,
        eventWatcher: SessionEventStreamWatcher
    ) {
        self.getRoomInfoUseCase = getRoomInfoUseCase
        self.getSessionSnapshotUseCase = getSessionSnapshotUseCase
        self.submitAnswerUseCase = submitAnswerUseCase
        self.getVoiceHintsUseCase = getVoiceHintsUseCase
        self.leaveRoomUseCase = leaveRoomUseCase
        self.getMyParticipationUseCase = getMyParticipationUseCase
        self.snapshotPolicy = snapshotPolicy
        self.eventWatcher = eventWatcher
        self.uiState = PlayUiState()
    }

    deinit {
        ticker?.invalidate()
        eventWatcher.stop()
    }
}
