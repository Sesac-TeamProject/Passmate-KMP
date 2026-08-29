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

    private func onEnter(pin: String) {
        if roomId != nil {
            return
        }
        let my = getMyParticipationUseCase.invoke()

        uiState.pin = pin
        uiState.myParticipantId = my?.participantId
        uiState.myNickname = my?.nickname
        getRoomInfoUseCase.invoke(pin: pin) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let success = result as? AppResultSuccess<AnyObject>

                if error == nil, let room = success?.value as? RoomInfo {
                    self.roomId = room.roomId
                    self.uiState.isLoading = false
                    self.uiState.roomTitle = room.title
                    self.observeRoomEvents(roomId: room.roomId)
                } else {
                    self.uiState.isLoading = false
                    self.event.send(.roomClosed(message: self.roomErrorMessage((result as? AppResultFailure)?.error)))
                }
            }
        }
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
            event.send(.sessionStarted(pin: uiState.pin))
        } else if serverEvent is ServerEventRoomCancelled {
            event.send(.roomClosed(message: "방이 취소됐어요"))
        }
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

        uiState.participants = others + [participant]
        uiState.totalCount = Int(joined.count)
    }

    private func onParticipantLeft(_ left: ServerEventParticipantLeft) {
        let isMe = left.participantId == uiState.myParticipantId
        let isKicked = left.reason == ServerEventParticipantLeft.companion.REASON_KICKED

        if isMe && isKicked {
            event.send(.roomClosed(message: "선생님이 내보냈어요"))
        } else {
            uiState.participants = uiState.participants.filter { $0.participantId != left.participantId }
            uiState.totalCount = Int(left.count)
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
