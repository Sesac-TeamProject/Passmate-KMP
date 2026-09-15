import Combine
import Foundation
import Shared

final class JoinViewModel: ObservableObject {
    private let getRoomInfoUseCase: GetRoomInfoUseCase

    private let joinRoomUseCase: JoinRoomUseCase

    private let rejoinRoomUseCase: RejoinRoomUseCase

    private let isSignedInUseCase: IsSignedInUseCase

    private let joinInputPolicy: JoinInputPolicy

    @Published private(set) var uiState: JoinUiState

    let event = PassthroughSubject<JoinEvent, Never>()

    private func onChangePin(pin: String) {
        let maxLength = Int(JoinInputPolicy.companion.PIN_LENGTH)
        let digits = String(pin.filter { $0.isNumber }.prefix(maxLength))

        uiState.pin = digits
        // PIN이 완성되면 방 정보(호스트 등급·별점)를 미리 불러온다, 바뀌면 초기화 (T081)
        if joinInputPolicy.isValidPin(pin: digits) {
            prefetchRoomInfo(pin: digits)
        } else {
            uiState.roomInfo = nil
            uiState.isLoadingRoomInfo = false
        }
    }

    private func prefetchRoomInfo(pin: String) {
        if uiState.roomInfo?.pin == pin {
            return
        }
        uiState.isLoadingRoomInfo = true
        getRoomInfoUseCase.invoke(pin: pin) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let success = result as? AppResultSuccess<AnyObject>

                self.uiState.isLoadingRoomInfo = false
                if error == nil, let room = success?.value as? RoomInfo {
                    self.uiState.roomInfo = room
                } else {
                    self.uiState.roomInfo = nil
                }
            }
        }
    }

    private func onChangeNickname(nickname: String) {
        let maxLength = Int(JoinInputPolicy.companion.NICKNAME_MAX_LENGTH)

        uiState.nickname = String(nickname.prefix(maxLength))
    }

    private func onSelectAvatar(avatarId: Int) {
        uiState.avatarId = avatarId
    }

    private func onClickScanQr() {
        event.send(.requestQrScan)
    }

    private func onReceiveQrResult(text: String?) {
        if let text {
            let pin = joinInputPolicy.extractPin(text: text)

            if let pin {
                uiState.pin = pin
            } else {
                event.send(.showNotice(message: "QR 코드에서 PIN을 찾지 못했어요"))
            }
        }
    }

    private func onClickSignIn() {
        event.send(.signInRequested)
    }

    private func onClickJoin() {
        let pin = uiState.pin
        let nickname = uiState.nickname

        if uiState.isJoining {
            return
        }
        if !joinInputPolicy.isValidPin(pin: pin) {
            event.send(.showNotice(message: "PIN 6자리를 입력해 주세요"))
        } else if !joinInputPolicy.isValidNickname(nickname: nickname) {
            event.send(.showNotice(message: "이 방에서 쓸 닉네임을 입력해 주세요"))
        } else {
            uiState.isJoining = true
            loadRoomAndJoin(pin: pin, nickname: nickname, avatarId: uiState.avatarId)
        }
    }

    private func loadRoomAndJoin(pin: String, nickname: String, avatarId: Int) {
        getRoomInfoUseCase.invoke(pin: pin) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let success = result as? AppResultSuccess<AnyObject>
                let failure = result as? AppResultFailure

                if error != nil {
                    self.uiState.isJoining = false
                    self.event.send(.showNotice(message: "입장하지 못했어요. 잠시 후 다시 시도해 주세요"))
                } else if let room = success?.value as? RoomInfo {
                    self.joinIfAllowed(room: room, nickname: nickname, avatarId: avatarId)
                } else {
                    self.uiState.isJoining = false
                    self.event.send(.showNotice(message: self.roomErrorMessage(failure?.error)))
                }
            }
        }
    }

    // 클라이언트 가드는 UX 목적 — 최종 판정은 서버 4xx를 그대로 처리한다 (규칙 §8)
    private func joinIfAllowed(room: RoomInfo, nickname: String, avatarId: Int) {
        if room.status == RoomStatus.finished {
            uiState.isJoining = false
            event.send(.showNotice(message: "이미 종료된 방이에요"))
        } else if room.isPaid && !uiState.isSignedIn {
            uiState.isJoining = false
            event.send(.showNotice(message: "유료 방은 로그인 후 입장할 수 있어요"))
            event.send(.signInRequiredForPaidRoom(pin: room.pin))
        } else if room.isPaid {
            // 회원의 유료 방 입장은 참가비 결제 화면으로 위임한다 (US14)
            uiState.isJoining = false
            event.send(.paymentRequired(pin: room.pin))
        } else {
            joinRoomUseCase.invoke(
                room: room,
                nickname: nickname,
                avatarId: KotlinInt(int: Int32(avatarId))
            ) { [weak self] result, error in
                DispatchQueue.main.async {
                    guard let self else { return }
                    self.uiState.isJoining = false
                    if error == nil, let success = result as? AppResultSuccess<AnyObject> {
                        // 입장에 성공하면 폼의 PIN을 비운다 — 대기실에서 나와 홈으로 돌아왔을 때
                        // 지난 방의 PIN이 남아 있으면 안 된다. 닉네임·캐릭터는 다음 입장에도 쓰므로 남긴다
                        self.uiState.pin = ""
                        self.uiState.roomInfo = nil
                        // 새 입장이 아니라 원래 자리로 돌아간 경우 — 입력한 이름이 아닌 처음 이름으로
                        // 들어가므로 말없이 바뀌면 혼란스럽다
                        if (success.value as? MyParticipation)?.isRejoined == true {
                            self.event.send(.showNotice(message: Self.resumedNotice))
                        }
                        self.event.send(.joinCompleted(pin: room.pin))
                    } else {
                        self.handleJoinFailure(room: room, error: (result as? AppResultFailure)?.error)
                    }
                }
            }
        }
    }

    // 403은 code로 원인을 가른다 — 새 입장과 재입장이 같은 문구를 써야 강퇴 안내가 경로마다 달라지지 않는다.
    // 입장 흐름에서 ACCESS_DENIED는 강퇴당한 방의 재입장 거부다 (규칙 §10)
    private func forbiddenMessage(_ code: String?) -> String {
        if code == ServerErrorCode.shared.HOST_CANNOT_JOIN {
            return "내가 만든 방에는 참가자로 입장할 수 없어요"
        } else if code == ServerErrorCode.shared.ACCESS_DENIED {
            return "내보내진 방에는 다시 들어올 수 없어요"
        } else {
            return "이 방에 입장할 권한이 없어요"
        }
    }

    // 재입장 실패는 원인별로 갈라야 한다 — 강퇴는 다시 눌러도 안 되고, 끝난 방은 결과로 가야 한다 (규칙 §10)
    private func rejoinErrorMessage(_ error: AppError?, fallbackMessage: String) -> String {
        if let forbidden = error as? AppError.PermissionDenied {
            return forbiddenMessage(forbidden.serverCode)
        } else if error is AppError.Gone {
            return "이미 종료된 방이에요"
        } else if error is AppError.NetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return fallbackMessage
        }
    }

    // 새 입장이 막힌 방(기입장·진행 중)은 재입장으로 원래 참가자 행을 되살려 들어간다.
    // 새 행을 만들지 않아야 참가자 id가 채워지고 점수·답안이 갈라지지 않는다 (규칙 §2-1-2 재접속 복구)
    private func enterAlreadyJoinedRoom(room: RoomInfo, fallbackMessage: String) {
        rejoinRoomUseCase.invoke(room: room) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                if error == nil, result is AppResultSuccess<AnyObject> {
                    self.uiState.pin = ""
                    self.uiState.roomInfo = nil
                    self.event.send(.showNotice(message: Self.resumedNotice))
                    self.event.send(.joinCompleted(pin: room.pin))
                } else {
                    let failure = (result as? AppResultFailure)?.error
                    self.event.send(.showNotice(message: self.rejoinErrorMessage(failure, fallbackMessage: fallbackMessage)))
                }
            }
        }
    }

    // 서버는 입장 409를 닉네임 중복·기입장·입장 불가·정원 초과 네 가지로 준다.
    // code로 갈라야 문구가 맞고, 닉네임을 바꿔도 안 들어가지는 상태가 안 생긴다.
    // 모르는 409(code 없음 포함)를 닉네임 중복이라고 하면 이유를 알 수 없으니 일반 실패로 둔다 (규칙 §10)
    private func handleJoinConflict(room: RoomInfo, conflict: AppError.Conflict) {
        let code = conflict.serverCode

        if code == ServerErrorCode.shared.ALREADY_JOINED {
            enterAlreadyJoinedRoom(
                room: room,
                fallbackMessage: "이미 입장해 있는 방인데 다시 들어가지 못했어요. 잠시 후 다시 시도해 주세요"
            )
        } else if code == ServerErrorCode.shared.ROOM_NOT_JOINABLE {
            // 진행 중인 방은 새로 입장할 수 없지만, 전에 들어갔던 사람은 돌아올 수 있다.
            // 들어간 적이 없으면 서버가 404를 주고 원래 문구로 돌아간다
            enterAlreadyJoinedRoom(
                room: room,
                fallbackMessage: "이미 시작했거나 끝난 방이라 입장할 수 없어요"
            )
        } else if code == ServerErrorCode.shared.ROOM_FULL {
            event.send(.showNotice(message: "정원이 가득 찼어요"))
        } else if code == ServerErrorCode.shared.NICKNAME_DUPLICATED {
            event.send(.showNotice(message: "이미 사용 중인 닉네임이에요. 다른 이름을 입력해 주세요"))
        } else {
            event.send(.showNotice(message: roomErrorMessage(conflict)))
        }
    }

    private func handleJoinFailure(room: RoomInfo, error: AppError?) {
        if let conflict = error as? AppError.Conflict {
            handleJoinConflict(room: room, conflict: conflict)
        } else if let forbidden = error as? AppError.PermissionDenied {
            event.send(.showNotice(message: forbiddenMessage(forbidden.serverCode)))
        } else if error is AppError.LoginRequired {
            event.send(.showNotice(message: "유료 방은 로그인 후 입장할 수 있어요"))
            event.send(.signInRequiredForPaidRoom(pin: uiState.pin))
        } else if error is AppError.PaymentRequired {
            event.send(.paymentRequired(pin: uiState.pin))
        } else {
            event.send(.showNotice(message: roomErrorMessage(error)))
        }
    }

    private func roomErrorMessage(_ error: AppError?) -> String {
        if error is AppError.NotFound {
            return "방을 찾을 수 없어요. PIN을 확인해 주세요"
        } else if error is AppError.Gone {
            return "이미 종료된 방이에요"
        } else if error is AppError.NetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return "입장하지 못했어요. 잠시 후 다시 시도해 주세요"
        }
    }

    func action(_ action: JoinAction) {
        switch action {
        case let .changePin(pin):
            onChangePin(pin: pin)
        case let .changeNickname(nickname):
            onChangeNickname(nickname: nickname)
        case let .selectAvatar(avatarId):
            onSelectAvatar(avatarId: avatarId)
        case .clickScanQr:
            onClickScanQr()
        case let .receiveQrResult(text):
            onReceiveQrResult(text: text)
        case .clickJoin:
            onClickJoin()
        case .clickSignIn:
            onClickSignIn()
        }
    }

    init(
        getRoomInfoUseCase: GetRoomInfoUseCase,
        joinRoomUseCase: JoinRoomUseCase,
        rejoinRoomUseCase: RejoinRoomUseCase,
        isSignedInUseCase: IsSignedInUseCase,
        joinInputPolicy: JoinInputPolicy
    ) {
        self.getRoomInfoUseCase = getRoomInfoUseCase
        self.joinRoomUseCase = joinRoomUseCase
        self.rejoinRoomUseCase = rejoinRoomUseCase
        self.isSignedInUseCase = isSignedInUseCase
        self.joinInputPolicy = joinInputPolicy
        self.uiState = JoinUiState(isSignedIn: isSignedInUseCase.invoke())
    }

    private static let resumedNotice = "이미 입장해 있는 방이에요. 처음 입장한 이름으로 이어서 들어갈게요"
}
