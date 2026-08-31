import Combine
import Foundation
import Shared

// Compose PaymentViewModel.kt 미러 — 보유 코인 확인 → 부족 시 포트원 충전 → 참가비 차감 → 입장.
// 최종 차감·자격 판정은 서버가 하며 여기 계산은 UX용이다 (규칙 §8·§13).
final class PaymentViewModel: ObservableObject {
    private let getRoomInfoUseCase: GetRoomInfoUseCase

    private let getMyCoinsUseCase: GetMyCoinsUseCase

    private let requestChargeUseCase: RequestChargeUseCase

    private let confirmChargeUseCase: ConfirmChargeUseCase

    private let payEntryFeeUseCase: PayEntryFeeUseCase

    private let joinRoomUseCase: JoinRoomUseCase

    private let coinPolicy: CoinPolicy

    private let joinInputPolicy: JoinInputPolicy

    @Published private(set) var uiState = PaymentUiState()

    let event = PassthroughSubject<PaymentEvent, Never>()

    private var pin: String = ""

    private var pendingChargeId: String? = nil

    private func onStart(pin: String) {
        if self.pin == pin && uiState.room != nil {
            return
        }
        self.pin = pin
        load()
    }

    private func load() {
        uiState.isLoading = true
        uiState.hasLoadError = false
        getRoomInfoUseCase.invoke(pin: pin) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                if let room = (result as? AppResultSuccess<AnyObject>)?.value as? RoomInfo {
                    self.loadCoins(room: room)
                } else {
                    self.uiState.isLoading = false
                    self.uiState.hasLoadError = true
                }
            }
        }
    }

    private func loadCoins(room: RoomInfo) {
        getMyCoinsUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                if let coins = (result as? AppResultSuccess<AnyObject>)?.value as? CoinBalance {
                    let balance = Int(coins.balance)
                    let entryFee = Int(room.entryFee?.int32Value ?? 0)

                    self.uiState.isLoading = false
                    self.uiState.hasLoadError = false
                    self.uiState.room = room
                    self.uiState.balance = balance
                    self.uiState.shortfall = Int(self.coinPolicy.shortfall(balance: Int32(balance), entryFee: Int32(entryFee)))
                    if let method = coins.defaultMethod {
                        self.uiState.selectedMethod = method
                    }
                } else {
                    self.uiState.isLoading = false
                    self.uiState.hasLoadError = true
                }
            }
        }
    }

    private func onChangeNickname(nickname: String) {
        let maxLength = Int(JoinInputPolicy.companion.NICKNAME_MAX_LENGTH)

        uiState.nickname = String(nickname.prefix(maxLength))
    }

    private func onClickPay() {
        guard let room = uiState.room, !uiState.isProcessing else {
            return
        }
        if !joinInputPolicy.isValidNickname(nickname: uiState.nickname) {
            event.send(.showNotice(message: "이 방에서 쓸 닉네임을 입력해 주세요"))
        } else if uiState.hasEnough {
            uiState.isProcessing = true
            uiState.errorMessage = nil
            payEntryAndEnter(room: room)
        } else {
            uiState.isProcessing = true
            uiState.errorMessage = nil
            startCharge()
        }
    }

    private func startCharge() {
        let amount = coinPolicy.suggestedChargeAmount(shortfall: Int32(uiState.shortfall))

        requestChargeUseCase.invoke(amount: amount, method: uiState.selectedMethod, roomId: nil) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                if let checkout = (result as? AppResultSuccess<AnyObject>)?.value as? CoinCheckout {
                    self.showPortOne(checkout: checkout)
                } else {
                    self.uiState.isProcessing = false
                    self.uiState.errorMessage = self.chargeErrorMessage((result as? AppResultFailure)?.error)
                }
            }
        }
    }

    private func showPortOne(checkout: CoinCheckout) {
        pendingChargeId = checkout.chargeId
        uiState.checkout = PortOneRequest(
            storeId: checkout.storeId,
            channelKey: checkout.channelKey,
            paymentId: checkout.paymentId,
            orderName: checkout.orderName,
            totalAmount: Int(checkout.amount),
            currency: checkout.currency,
            payMethod: checkout.payMethod
        )
    }

    private func onReceivePortOneResult(result: PortOneResult) {
        uiState.checkout = nil

        switch result {
        case let .success(paymentId):
            confirmAndEnter(paymentId: paymentId)
        case let .failure(message):
            uiState.isProcessing = false
            uiState.errorMessage = message
        case .cancelled:
            uiState.isProcessing = false
            uiState.errorMessage = nil
        }
    }

    private func confirmAndEnter(paymentId: String) {
        guard let chargeId = pendingChargeId, let room = uiState.room else {
            uiState.isProcessing = false
            uiState.errorMessage = "결제 정보를 확인하지 못했어요. 다시 시도해 주세요"
            return
        }
        confirmChargeUseCase.invoke(chargeId: chargeId, paymentId: paymentId, roomId: nil) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                if let confirm = (result as? AppResultSuccess<AnyObject>)?.value as? ChargeConfirm {
                    let balance = Int(confirm.balance)
                    let entryFee = Int(room.entryFee?.int32Value ?? 0)

                    self.uiState.balance = balance
                    self.uiState.shortfall = Int(self.coinPolicy.shortfall(balance: Int32(balance), entryFee: Int32(entryFee)))
                    self.payEntryAndEnter(room: room)
                } else {
                    self.uiState.isProcessing = false
                    self.uiState.errorMessage = self.chargeErrorMessage((result as? AppResultFailure)?.error)
                }
            }
        }
    }

    private func payEntryAndEnter(room: RoomInfo) {
        payEntryFeeUseCase.invoke(
            roomId: room.roomId,
            nickname: uiState.nickname,
            avatarId: KotlinInt(int: Int32(uiState.avatarId))
        ) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                if result is AppResultSuccess<AnyObject> {
                    self.enterRoom(room: room)
                } else {
                    self.handleEntryFailure(room: room, error: (result as? AppResultFailure)?.error)
                }
            }
        }
    }

    private func handleEntryFailure(room: RoomInfo, error: AppError?) {
        if error is AppError.PaymentRequired {
            let entryFee = Int(room.entryFee?.int32Value ?? 0)

            uiState.shortfall = Int(coinPolicy.shortfall(balance: Int32(uiState.balance), entryFee: Int32(entryFee)))
            startCharge()
        } else if error is AppError.LoginRequired || error is AppError.Unauthorized {
            uiState.isProcessing = false
            event.send(.signInRequired)
        } else {
            uiState.isProcessing = false
            uiState.errorMessage = entryErrorMessage(error)
        }
    }

    private func enterRoom(room: RoomInfo) {
        joinRoomUseCase.invoke(
            room: room,
            nickname: uiState.nickname,
            avatarId: KotlinInt(int: Int32(uiState.avatarId))
        ) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isProcessing = false
                if error == nil, result is AppResultSuccess<AnyObject> {
                    self.event.send(.enterRoom(pin: room.pin))
                } else {
                    self.uiState.errorMessage = self.entryErrorMessage((result as? AppResultFailure)?.error)
                }
            }
        }
    }

    private func chargeErrorMessage(_ error: AppError?) -> String {
        if error is AppError.NetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return error?.serverMessage ?? "결제에 실패했어요. 다시 시도해 주세요"
        }
    }

    private func entryErrorMessage(_ error: AppError?) -> String {
        if error is AppError.Conflict {
            return "이미 사용 중인 닉네임이에요. 다른 이름을 입력해 주세요"
        } else if error is AppError.Gone {
            return "이미 종료된 방이에요"
        } else if error is AppError.NetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return error?.serverMessage ?? "입장하지 못했어요. 잠시 후 다시 시도해 주세요"
        }
    }

    func action(_ action: PaymentAction) {
        switch action {
        case let .start(pin):
            onStart(pin: pin)
        case let .changeNickname(nickname):
            onChangeNickname(nickname: nickname)
        case let .selectAvatar(avatarId):
            uiState.avatarId = avatarId
        case let .selectMethod(method):
            uiState.selectedMethod = method
        case .clickPay:
            onClickPay()
        case let .receivePortOneResult(result):
            onReceivePortOneResult(result: result)
        case .dismissError:
            uiState.errorMessage = nil
        case .retry:
            load()
        }
    }

    init(
        getRoomInfoUseCase: GetRoomInfoUseCase,
        getMyCoinsUseCase: GetMyCoinsUseCase,
        requestChargeUseCase: RequestChargeUseCase,
        confirmChargeUseCase: ConfirmChargeUseCase,
        payEntryFeeUseCase: PayEntryFeeUseCase,
        joinRoomUseCase: JoinRoomUseCase,
        coinPolicy: CoinPolicy,
        joinInputPolicy: JoinInputPolicy
    ) {
        self.getRoomInfoUseCase = getRoomInfoUseCase
        self.getMyCoinsUseCase = getMyCoinsUseCase
        self.requestChargeUseCase = requestChargeUseCase
        self.confirmChargeUseCase = confirmChargeUseCase
        self.payEntryFeeUseCase = payEntryFeeUseCase
        self.joinRoomUseCase = joinRoomUseCase
        self.coinPolicy = coinPolicy
        self.joinInputPolicy = joinInputPolicy
    }
}
