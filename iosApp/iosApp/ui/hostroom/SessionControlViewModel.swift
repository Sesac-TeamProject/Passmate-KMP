import Combine
import Foundation
import Shared

// Compose SessionControlViewModel.kt 미러 — 호스트 세션 제어 (M-T2 리모컨)
final class SessionControlViewModel: ObservableObject {
    private let getRoomInfoUseCase: GetRoomInfoUseCase

    private let getSessionSnapshotUseCase: GetSessionSnapshotUseCase

    private let getSubmissionsUseCase: GetSubmissionsUseCase

    private let startSessionUseCase: StartSessionUseCase

    private let nextQuestionUseCase: NextQuestionUseCase

    private let endCurrentQuestionUseCase: EndCurrentQuestionUseCase

    private let endSessionUseCase: EndSessionUseCase

    private let setScreenLockUseCase: SetScreenLockUseCase

    private let eventWatcher: SessionEventStreamWatcher

    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = SessionControlUiState()

    let event = PassthroughSubject<SessionControlEvent, Never>()

    private var roomId: Int64 = -1

    private var pin = ""

    private var deadline: QuestionDeadline?

    private var ticker: Timer?

    private func onEnter(roomId: Int64, pin: String) {
        if self.roomId == roomId {
            return
        }
        self.roomId = roomId
        self.pin = pin
        // 호스트(회원) 전용 가드 — 호스트 검증은 서버 403이 최종 권위 (규칙 §8)
        if isSignedInUseCase.invoke() {
            load()
            observeEvents()
        } else {
            event.send(.requireSignIn)
        }
    }

    private func load() {
        uiState.isLoading = true
        uiState.loadFailed = false
        getRoomInfoUseCase.invoke(pin: pin) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let room = (result as? AppResultSuccess<AnyObject>)?.value as? RoomInfo

                if error == nil, let room {
                    self.uiState.roomTitle = room.title
                    self.uiState.pin = room.pin
                    self.uiState.participantCount = room.participantCount.map { Int(truncating: $0) } ?? 0
                    self.uiState.questionCount = room.questionCount.map { Int(truncating: $0) }
                    self.refreshSnapshot(isFirstLoad: true)
                } else {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = true
                }
            }
        }
    }

    // 재접속 프로토콜(규칙 §2-1-2) — Connected 수신 시에도 호출해 스냅샷으로 상태를 복구한다
    private func refreshSnapshot(isFirstLoad: Bool) {
        getSessionSnapshotUseCase.invoke(roomId: roomId) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let snapshot = (result as? AppResultSuccess<AnyObject>)?.value as? SessionSnapshot

                if error == nil, let snapshot {
                    let question = snapshot.currentQuestion

                    if let question, !question.isClosed {
                        self.deadline = QuestionDeadline.companion.fromServerTimes(endsAt: question.endsAt, serverNow: snapshot.ts)
                    } else {
                        self.deadline = nil
                    }
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = false
                    self.uiState.status = snapshot.status
                    self.uiState.questionCount = snapshot.questionCount.map { Int(truncating: $0) } ?? self.uiState.questionCount
                    self.uiState.question = question
                    self.uiState.isQuestionClosed = question?.isClosed ?? false
                    self.uiState.isLocked = snapshot.isLocked
                    self.restartTicker()
                    if snapshot.status == RoomStatus.running, question != nil {
                        self.refreshSubmissions()
                    }
                } else if isFirstLoad {
                    // 세션 미시작 방은 스냅샷이 없을 수 있다 — 대기(WAITING) 상태로 취급
                    if (result as? AppResultFailure)?.error is AppErrorNotFound {
                        self.uiState.isLoading = false
                        self.uiState.loadFailed = false
                        self.uiState.status = RoomStatus.waiting
                    } else {
                        self.uiState.isLoading = false
                        self.uiState.loadFailed = true
                    }
                }
            }
        }
    }

    private func refreshSubmissions() {
        getSubmissionsUseCase.invoke(roomId: roomId) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                // 제출 현황은 부가 정보 — 실패해도 리모컨 제어를 막지 않는다
                if error == nil, let submissions = (result as? AppResultSuccess<AnyObject>)?.value as? SubmissionStatus {
                    self.uiState.submissions = submissions
                }
            }
        }
    }

    private func observeEvents() {
        eventWatcher.startAsHost(roomId: roomId) { [weak self] streamEvent in
            guard let self else { return }
            if streamEvent is SessionEventStreamStreamEventConnected {
                self.refreshSnapshot(isFirstLoad: false)
            } else if let received = streamEvent as? SessionEventStreamStreamEventReceived {
                self.handleFrame(received.frame)
            }
        }
    }

    private func handleFrame(_ frame: ServerEventFrame) {
        let serverEvent = frame.event

        if let started = serverEvent as? ServerEventSessionStarted {
            uiState.status = RoomStatus.running
            uiState.questionCount = Int(started.questionCount)
        } else if let questionStarted = serverEvent as? ServerEventQuestionStarted {
            onQuestionStarted(questionStarted, serverTs: frame.ts)
        } else if serverEvent is ServerEventQuestionEnded {
            uiState.isQuestionClosed = true
            refreshSubmissions()
        } else if serverEvent is ServerEventAnswerSubmitted || serverEvent is ServerEventSubmissionUpdated {
            refreshSubmissions()
        } else if let locked = serverEvent as? ServerEventScreenLocked {
            uiState.isLocked = locked.locked
        } else if let joined = serverEvent as? ServerEventParticipantJoined {
            uiState.participantCount = Int(joined.count)
        } else if let left = serverEvent as? ServerEventParticipantLeft {
            uiState.participantCount = Int(left.count)
        } else if serverEvent is ServerEventProjectorConnected {
            uiState.isProjectorConnected = true
        } else if serverEvent is ServerEventProjectorDisconnected {
            uiState.isProjectorConnected = false
        } else if serverEvent is ServerEventSessionEnded {
            uiState.status = RoomStatus.finished
            event.send(.sessionEnded(roomId: roomId))
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
        uiState.status = RoomStatus.running
        uiState.question = question
        uiState.isQuestionClosed = false
        uiState.submissions = nil
        restartTicker()
        refreshSubmissions()
    }

    private func restartTicker() {
        stopTicker()
        if deadline != nil {
            uiState.remainingSec = deadline.map { Int($0.remainingSeconds()) } ?? 0
            ticker = Timer.scheduledTimer(withTimeInterval: 0.2, repeats: true) { [weak self] _ in
                guard let self else { return }
                if let deadline = self.deadline {
                    self.uiState.remainingSec = Int(deadline.remainingSeconds())
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

    private func onToggleLock() {
        let nextLocked = !uiState.isLocked

        runControl { [weak self] completion in
            self?.setScreenLockUseCase.invoke(roomId: self?.roomId ?? -1, locked: nextLocked) { result, error in
                if error == nil, result is AppResultSuccess<AnyObject> {
                    // SCREEN_LOCKED 브로드캐스트가 최종 상태지만 UX상 낙관 반영한다
                    self?.uiState.isLocked = nextLocked
                }
                completion(result, error)
            }
        }
    }

    // 제어 요청 공통 처리 — 상태 전이는 서버 브로드캐스트로만 반영한다 (규칙 §2-1-2)
    private func runControl(_ block: @escaping (@escaping (Any?, Error?) -> Void) -> Void) {
        if uiState.isControlling {
            return
        }
        uiState.isControlling = true
        block { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isControlling = false
                if error != nil || result is AppResultFailure {
                    let appError = (result as? AppResultFailure)?.error

                    self.event.send(.showNotice(message: self.controlFailMessage(appError)))
                }
            }
        }
    }

    // 서버 code 기반 문구 분기 (규칙 §10)
    private func controlFailMessage(_ error: AppError?) -> String {
        if error is AppErrorConflict {
            return "확정된 문제 세트를 먼저 연결해 주세요"
        } else if error is AppErrorPermissionDenied {
            return "방을 만든 선생님만 진행할 수 있어요"
        } else if error is AppErrorNetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return "요청을 처리하지 못했어요. 다시 시도해 주세요"
        }
    }

    func action(_ action: SessionControlAction) {
        switch action {
        case let .enter(roomId, pin):
            onEnter(roomId: roomId, pin: pin)
        case .retry:
            load()
        case .clickStart:
            runControl { [weak self] completion in
                self?.startSessionUseCase.invoke(roomId: self?.roomId ?? -1) { result, error in
                    completion(result, error)
                }
            }
        case .clickNext:
            runControl { [weak self] completion in
                self?.nextQuestionUseCase.invoke(roomId: self?.roomId ?? -1) { result, error in
                    completion(result, error)
                }
            }
        case .clickEndQuestion:
            runControl { [weak self] completion in
                self?.endCurrentQuestionUseCase.invoke(roomId: self?.roomId ?? -1) { result, error in
                    completion(result, error)
                }
            }
        case .confirmEndSession:
            runControl { [weak self] completion in
                self?.endSessionUseCase.invoke(roomId: self?.roomId ?? -1) { result, error in
                    completion(result, error)
                }
            }
        case .toggleLock:
            onToggleLock()
        }
    }

    deinit {
        stopTicker()
        eventWatcher.stop()
    }

    init(
        getRoomInfoUseCase: GetRoomInfoUseCase,
        getSessionSnapshotUseCase: GetSessionSnapshotUseCase,
        getSubmissionsUseCase: GetSubmissionsUseCase,
        startSessionUseCase: StartSessionUseCase,
        nextQuestionUseCase: NextQuestionUseCase,
        endCurrentQuestionUseCase: EndCurrentQuestionUseCase,
        endSessionUseCase: EndSessionUseCase,
        setScreenLockUseCase: SetScreenLockUseCase,
        eventWatcher: SessionEventStreamWatcher,
        isSignedInUseCase: IsSignedInUseCase
    ) {
        self.getRoomInfoUseCase = getRoomInfoUseCase
        self.getSessionSnapshotUseCase = getSessionSnapshotUseCase
        self.getSubmissionsUseCase = getSubmissionsUseCase
        self.startSessionUseCase = startSessionUseCase
        self.nextQuestionUseCase = nextQuestionUseCase
        self.endCurrentQuestionUseCase = endCurrentQuestionUseCase
        self.endSessionUseCase = endSessionUseCase
        self.setScreenLockUseCase = setScreenLockUseCase
        self.eventWatcher = eventWatcher
        self.isSignedInUseCase = isSignedInUseCase
    }
}
