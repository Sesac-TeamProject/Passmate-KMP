import Combine
import Foundation
import Shared

// Compose PaymentMethodViewModel.kt 미러 — 기본 결제 수단 선택·저장 (M-12-8)
final class PaymentMethodViewModel: ObservableObject {
    private let getMyCoinsUseCase: GetMyCoinsUseCase

    private let setPaymentMethodUseCase: SetPaymentMethodUseCase

    @Published private(set) var uiState = PaymentMethodUiState()

    let event = PassthroughSubject<PaymentMethodEvent, Never>()

    private var hasEntered = false

    private func onEnter() {
        if hasEntered {
            return
        }
        hasEntered = true
        loadDefault()
    }

    // 현재 기본 결제 수단은 내 코인 조회 응답의 defaultMethod 재사용 (GET /users/me/coins)
    private func loadDefault() {
        uiState.isLoading = true
        getMyCoinsUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isLoading = false
                // 기본값 로드 실패는 빈 선택으로 시작 — 저장 자체는 막지 않는다
                if error == nil, let balance = (result as? AppResultSuccess<AnyObject>)?.value as? CoinBalance {
                    if self.uiState.selected == nil {
                        self.uiState.selected = balance.defaultMethod
                    }
                }
            }
        }
    }

    private func onSubmit() {
        guard uiState.canSubmit, let selected = uiState.selected else { return }
        uiState.isSubmitting = true
        setPaymentMethodUseCase.invoke(method: selected) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isSubmitting = false
                if error == nil, result is AppResultSuccess<AnyObject> {
                    self.event.send(.saved)
                } else {
                    self.event.send(.showNotice(message: "결제 수단을 저장하지 못했어요. 다시 시도해 주세요"))
                }
            }
        }
    }

    func action(_ action: PaymentMethodAction) {
        switch action {
        case .enter:
            onEnter()
        case let .select(method):
            uiState.selected = method
        case .submit:
            onSubmit()
        }
    }

    init(
        getMyCoinsUseCase: GetMyCoinsUseCase,
        setPaymentMethodUseCase: SetPaymentMethodUseCase
    ) {
        self.getMyCoinsUseCase = getMyCoinsUseCase
        self.setPaymentMethodUseCase = setPaymentMethodUseCase
    }
}
