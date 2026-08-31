import Combine
import Foundation
import Shared

// Compose CoinChargeViewModel.kt 미러 — 보유 코인 확인 → 금액·수단 선택 → 포트원 충전 → 완료 표시.
// 최종 잔액은 서버 confirm 응답을 그대로 쓴다 — 클라이언트가 더하지 않는다 (규칙 §1 서버 권위)
final class CoinChargeViewModel: ObservableObject {
    private let getMyCoinsUseCase: GetMyCoinsUseCase

    private let requestChargeUseCase: RequestChargeUseCase

    private let confirmChargeUseCase: ConfirmChargeUseCase

    private let coinPolicy: CoinPolicy

    @Published private(set) var uiState = CoinChargeUiState()

    let event = PassthroughSubject<CoinChargeEvent, Never>()

    private var pendingChargeId: String?

    private func load() {
        uiState.isLoading = true
        uiState.hasLoadError = false
        uiState.presets = coinPolicy.presets.map { Int(truncating: $0) }
        getMyCoinsUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isLoading = false
                if let coins = (result as? AppResultSuccess<AnyObject>)?.value as? CoinBalance {
                    self.uiState.hasLoadError = false
                    self.uiState.balance = Int(coins.balance)
                    self.uiState.selectedMethod = coins.defaultMethod ?? self.uiState.selectedMethod
                } else {
                    self.uiState.hasLoadError = true
                }
            }
        }
    }

    private func onClickCharge() {
        if uiState.isProcessing || uiState.isLoading {
            return
        }
        uiState.isProcessing = true
        uiState.errorMessage = nil
        startCharge(amount: uiState.selectedAmount)
    }

    private func startCharge(amount: Int) {
        requestChargeUseCase.invoke(
            amount: Int32(amount),
            method: uiState.selectedMethod,
            roomId: nil
        ) { [weak self] result, error in
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
            confirmCharge(paymentId: paymentId)
        case let .failure(message):
            uiState.isProcessing = false
            uiState.errorMessage = message
        case .cancelled:
            uiState.isProcessing = false
            uiState.errorMessage = nil
        }
    }

    private func confirmCharge(paymentId: String) {
        let amount = uiState.selectedAmount

        guard let chargeId = pendingChargeId else {
            uiState.isProcessing = false
            uiState.errorMessage = "결제 정보를 확인하지 못했어요. 다시 시도해 주세요"
            return
        }
        confirmChargeUseCase.invoke(chargeId: chargeId, paymentId: paymentId, roomId: nil) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isProcessing = false
                if let confirm = (result as? AppResultSuccess<AnyObject>)?.value as? ChargeConfirm {
                    self.pendingChargeId = nil
                    self.uiState.isCompleted = true
                    self.uiState.balance = Int(confirm.balance)
                    self.uiState.chargedAmount = amount
                } else {
                    self.uiState.errorMessage = self.chargeErrorMessage((result as? AppResultFailure)?.error)
                }
            }
        }
    }

    private func chargeErrorMessage(_ error: AppError?) -> String {
        if error is AppError.NetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return error?.serverMessage ?? "충전에 실패했어요. 다시 시도해 주세요"
        }
    }

    func action(_ action: CoinChargeAction) {
        switch action {
        case .enter:
            load()
        case .retry:
            load()
        case let .selectAmount(amount):
            uiState.selectedAmount = amount
        case let .selectMethod(method):
            uiState.selectedMethod = method
        case .clickCharge:
            onClickCharge()
        case let .receivePortOneResult(result):
            onReceivePortOneResult(result: result)
        case .clickConfirmDone:
            event.send(.done)
        case .dismissError:
            uiState.errorMessage = nil
        }
    }

    init(
        getMyCoinsUseCase: GetMyCoinsUseCase,
        requestChargeUseCase: RequestChargeUseCase,
        confirmChargeUseCase: ConfirmChargeUseCase,
        coinPolicy: CoinPolicy
    ) {
        self.getMyCoinsUseCase = getMyCoinsUseCase
        self.requestChargeUseCase = requestChargeUseCase
        self.confirmChargeUseCase = confirmChargeUseCase
        self.coinPolicy = coinPolicy
    }
}
