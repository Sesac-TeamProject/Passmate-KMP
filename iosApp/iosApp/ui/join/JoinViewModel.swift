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
            event.send(.signInRequested)
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
                        self.event.send(.joinCompleted(pin: room.pin))
                    } else {
                        self.handleJoinFailure(error: (result as? AppResultFailure)?.error)
                    }
                }
            }
        }
    }

    private func handleJoinFailure(error: AppError?) {
        if error is AppErrorConflict {
            event.send(.showNotice(message: "이미 사용 중인 닉네임이에요. 다른 이름을 입력해 주세요"))
        } else if error is AppErrorLoginRequired {
            event.send(.showNotice(message: "유료 방은 로그인 후 입장할 수 있어요"))
            event.send(.signInRequested)
        } else if error is AppErrorPaymentRequired {
            event.send(.paymentRequired(pin: uiState.pin))
        } else {
            event.send(.showNotice(message: roomErrorMessage(error)))
        }
    }

    private func roomErrorMessage(_ error: AppError?) -> String {
        if error is AppErrorNotFound {
            return "방을 찾을 수 없어요. PIN을 확인해 주세요"
        } else if error is AppErrorGone {
            return "이미 종료된 방이에요"
        } else if error is AppErrorNetworkError {
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
}
