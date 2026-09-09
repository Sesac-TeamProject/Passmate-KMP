import Combine
import Foundation
import Shared

final class JoinViewModel: ObservableObject {
    private let getRoomInfoUseCase: GetRoomInfoUseCase

    private let joinRoomUseCase: JoinRoomUseCase

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
                    if error == nil, result is AppResultSuccess<AnyObject> {
                        // 입장에 성공하면 폼의 PIN을 비운다 — 대기실에서 나와 홈으로 돌아왔을 때
                        // 지난 방의 PIN이 남아 있으면 안 된다. 닉네임·캐릭터는 다음 입장에도 쓰므로 남긴다
                        self.uiState.pin = ""
                        self.uiState.roomInfo = nil
                        self.event.send(.joinCompleted(pin: room.pin))
                    } else {
                        self.handleJoinFailure(room: room, error: (result as? AppResultFailure)?.error)
                    }
                }
            }
        }
    }

    // 회원은 이미 자기 참가자 행이 살아 있어 두 번째 입장이 막힌다(게스트는 매번 새 행이라 안 걸린다).
    // 막을 일이 아니라 이미 들어와 있는 것이므로 그대로 들여보낸다 (규칙 §2-1-2 재접속 복구)
    private func enterAlreadyJoinedRoom(pin: String) {
        uiState.pin = ""
        uiState.roomInfo = nil
        event.send(.showNotice(message: "이미 입장해 있는 방이에요. 처음 입장한 이름으로 이어서 들어갈게요"))
        event.send(.joinCompleted(pin: pin))
    }

    // 서버는 409를 닉네임 중복·기입장·입장 불가·정원 초과 네 가지로 준다.
    // code로 갈라야 문구가 맞고, 닉네임을 바꿔도 안 들어가지는 상태가 안 생긴다 (규칙 §10)
    private func handleJoinConflict(room: RoomInfo, code: String?) {
        if code == Self.alreadyJoined {
            enterAlreadyJoinedRoom(pin: room.pin)
        } else if code == Self.roomNotJoinable {
            event.send(.showNotice(message: "이미 시작했거나 끝난 방이라 입장할 수 없어요"))
        } else if code == Self.roomFull {
            event.send(.showNotice(message: "정원이 가득 찼어요"))
        } else {
            event.send(.showNotice(message: "이미 사용 중인 닉네임이에요. 다른 이름을 입력해 주세요"))
        }
    }

    private func handleJoinFailure(room: RoomInfo, error: AppError?) {
        if let conflict = error as? AppError.Conflict {
            handleJoinConflict(room: room, code: conflict.serverCode)
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
        isSignedInUseCase: IsSignedInUseCase,
        joinInputPolicy: JoinInputPolicy
    ) {
        self.getRoomInfoUseCase = getRoomInfoUseCase
        self.joinRoomUseCase = joinRoomUseCase
        self.isSignedInUseCase = isSignedInUseCase
        self.joinInputPolicy = joinInputPolicy
        self.uiState = JoinUiState(isSignedIn: isSignedInUseCase.invoke())
    }

    // 입장 409의 서버 코드 (contracts/rest-api.md)
    private static let alreadyJoined = "ALREADY_JOINED"

    private static let roomNotJoinable = "ROOM_NOT_JOINABLE"

    private static let roomFull = "ROOM_FULL"
}
