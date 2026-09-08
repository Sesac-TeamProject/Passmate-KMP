import Combine
import Foundation
import Shared

final class WaitingViewModel: ObservableObject {
    private let getRoomInfoUseCase: GetRoomInfoUseCase

    private let getParticipantsUseCase: GetParticipantsUseCase

    private let leaveRoomUseCase: LeaveRoomUseCase

    private let getMyParticipationUseCase: GetMyParticipationUseCase

    private let eventWatcher: SessionEventStreamWatcher

    @Published private(set) var uiState: WaitingUiState

    let event = PassthroughSubject<WaitingEvent, Never>()

    private var roomId: Int64?

    // 풀이 화면으로 넘긴 뒤인가 — 넘긴 뒤라면 세션 종료로 사용자를 끌어내지 않는다
    private var hasHandedOffToPlay = false

    private func onEnter(pin: String) {
        let isReturning = roomId != nil

        if isReturning {
            recheckStatus(pin: pin)
        } else {
            loadRoom(pin: pin)
        }
    }

    private func loadRoom(pin: String) {
        let my = getMyParticipationUseCase.invoke()

        uiState.pin = pin
        uiState.myParticipantId = my?.participantId
        uiState.myNickname = my?.nickname
        getRoomInfoUseCase.invoke(pin: pin) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let success = result as? AppResultSuccess<AnyObject>

                if error == nil, let room = success?.value as? RoomInfo {
                    self.uiState.isLoading = false
                    self.uiState.roomTitle = room.title
                    self.routeByStatus(room: room, pin: pin)
                } else {
                    self.uiState.isLoading = false
                    self.event.send(.roomClosed(message: self.roomErrorMessage((result as? AppResultFailure)?.error)))
                }
            }
        }
    }

    // 풀이 화면에서 뒤로가기로 돌아온 경로다. 그 사이 끝난 세션이면 "입장 완료" 화면에 머무르면 안 된다.
    // 이미 받아 둔 종료 신호가 있으면 즉시, 없으면 서버 상태를 다시 확인한다 (규칙 §2-1-2 재접속 복구).
    // 진행 중(RUNNING)이면 다시 풀이로 밀어 넣지 않는다 — 뒤로가기가 영영 먹히지 않게 된다
    private func recheckStatus(pin: String) {
        hasHandedOffToPlay = false
        if uiState.isSessionFinished, let knownRoomId = roomId {
            emitSessionFinished(roomId: knownRoomId)
        } else {
            getRoomInfoUseCase.invoke(pin: pin) { [weak self] result, error in
                DispatchQueue.main.async {
                    guard let self else { return }
                    let success = result as? AppResultSuccess<AnyObject>

                    if error == nil, let room = success?.value as? RoomInfo {
                        if room.status == RoomStatus.finished {
                            self.emitSessionFinished(roomId: room.roomId)
                        } else {
                            self.observeRoomEvents(roomId: room.roomId)
                        }
                    } else {
                        self.event.send(.roomClosed(message: self.roomErrorMessage((result as? AppResultFailure)?.error)))
                    }
                }
            }
        }
    }

    // 첫 진입의 라우트는 서버 상태가 정한다 (규칙 §2-1-2) —
    // WAITING은 대기실 유지, RUNNING은 늦은 입장(FR-024), FINISHED는 결과 화면
    private func routeByStatus(room: RoomInfo, pin: String) {
        roomId = room.roomId

        if room.status == RoomStatus.running {
            emitSessionStarted(pin: pin)
        } else if room.status == RoomStatus.finished {
            emitSessionFinished(roomId: room.roomId)
        } else {
            observeRoomEvents(roomId: room.roomId)
        }
    }

    private func emitSessionStarted(pin: String) {
        hasHandedOffToPlay = true
        event.send(.sessionStarted(pin: pin))
    }

    // 종료 사실을 상태로도 남긴다 — 대기실이 스택에 있는 동안 보낸 event는 아무도 받지 못하므로
    // 되돌아오는 순간 recheckStatus가 이 값을 보고 다시 내보낸다
    private func emitSessionFinished(roomId finishedRoomId: Int64) {
        uiState.isLoading = false
        uiState.isSessionFinished = true
        event.send(.sessionFinished(roomId: finishedRoomId))
    }

    // 초기 목록·재접속 복구는 REST 조회, 이후 증분은 WS 이벤트 (규칙 §2-1-2)
    private func observeRoomEvents(roomId: Int64) {
        eventWatcher.start(roomId: roomId) { [weak self] streamEvent in
            guard let self else { return }
            if streamEvent is SessionEventStreamStreamEventConnected {
                self.refreshParticipants(roomId: roomId)
            } else if let received = streamEvent as? SessionEventStreamStreamEventReceived {
                self.handleServerEvent(received.frame.event)
            }
        }
    }

    private func refreshParticipants(roomId: Int64) {
        getParticipantsUseCase.invoke(roomId: roomId) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let success = result as? AppResultSuccess<AnyObject>

                if error == nil, let participants = success?.value as? [Participant] {
                    self.uiState.isLoading = false
                    self.uiState.participants = participants
                    self.uiState.totalCount = participants.count
                }
            }
        }
    }

    private func handleServerEvent(_ serverEvent: ServerEvent) {
        if let joined = serverEvent as? ServerEventParticipantJoined {
            onParticipantJoined(joined)
        } else if let left = serverEvent as? ServerEventParticipantLeft {
            onParticipantLeft(left)
        } else if serverEvent is ServerEventSessionStarted {
            emitSessionStarted(pin: uiState.pin)
        } else if serverEvent is ServerEventSessionEnded {
            // 대기실에 있는 동안 세션이 끝났다 (선생님이 시작 없이 종료한 경우 포함)
            onSessionEnded()
        } else if serverEvent is ServerEventRoomCancelled {
            event.send(.roomClosed(message: "방이 취소됐어요"))
        }
    }

    private func onSessionEnded() {
        guard let endedRoomId = roomId, !hasHandedOffToPlay else {
            // 풀이 화면이 앞에 있다 — 사용자가 보고 있는 최종 순위(M-05)를 가로채지 않고 상태만 남긴다.
            // 뒤로가기로 대기실에 돌아오는 순간 recheckStatus가 이 값을 보고 결과로 보낸다
            uiState.isSessionFinished = true
            return
        }
        emitSessionFinished(roomId: endedRoomId)
    }

    private func onParticipantJoined(_ joined: ServerEventParticipantJoined) {
        let participant = Participant(
            participantId: joined.participantId,
            nickname: joined.nickname,
            avatarId: joined.avatarId,
            isGuest: joined.isGuest,
            isConnected: true
        )
        let others = uiState.participants.filter { $0.participantId != participant.participantId }

        // 서버가 현재 인원을 안 실어 준다 — 명단 길이로 센다
        uiState.participants = others + [participant]
        uiState.totalCount = uiState.participants.count
    }

    private func onParticipantLeft(_ left: ServerEventParticipantLeft) {
        let isMe = left.participantId == uiState.myParticipantId
        let isKicked = left.reason == ServerEventParticipantLeft.companion.REASON_KICKED

        if isMe && isKicked {
            event.send(.roomClosed(message: "선생님이 내보냈어요"))
        } else {
            uiState.participants = uiState.participants.filter { $0.participantId != left.participantId }
            uiState.totalCount = uiState.participants.count
        }
    }

    private func onClickLeave() {
        let leavingRoomId = roomId

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

    private func roomErrorMessage(_ error: AppError?) -> String {
        if error is AppError.NotFound {
            return "방을 찾을 수 없어요"
        } else if error is AppError.Gone {
            return "이미 종료된 방이에요"
        } else if error is AppError.NetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return "대기실 정보를 불러오지 못했어요"
        }
    }

    func action(_ action: WaitingAction) {
        switch action {
        case let .enter(pin):
            onEnter(pin: pin)
        case .clickLeave:
            onClickLeave()
        }
    }

    func stopWatching() {
        eventWatcher.stop()
    }

    init(
        getRoomInfoUseCase: GetRoomInfoUseCase,
        getParticipantsUseCase: GetParticipantsUseCase,
        leaveRoomUseCase: LeaveRoomUseCase,
        getMyParticipationUseCase: GetMyParticipationUseCase,
        eventWatcher: SessionEventStreamWatcher
    ) {
        self.getRoomInfoUseCase = getRoomInfoUseCase
        self.getParticipantsUseCase = getParticipantsUseCase
        self.leaveRoomUseCase = leaveRoomUseCase
        self.getMyParticipationUseCase = getMyParticipationUseCase
        self.eventWatcher = eventWatcher
        self.uiState = WaitingUiState()
    }

    deinit {
        eventWatcher.stop()
    }
}
