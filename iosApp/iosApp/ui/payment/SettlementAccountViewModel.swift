import Combine
import Foundation
import Shared

// Compose SettlementAccountViewModel.kt 미러 — 정산 계좌 조회·저장 (M-12-3)
final class SettlementAccountViewModel: ObservableObject {
    private let getSettlementAccountUseCase: GetSettlementAccountUseCase

    private let saveSettlementAccountUseCase: SaveSettlementAccountUseCase

    @Published private(set) var uiState = SettlementAccountUiState()

    let event = PassthroughSubject<SettlementAccountEvent, Never>()

    private var hasEntered = false

    private func onEnter() {
        if hasEntered {
            return
        }
        hasEntered = true
        load()
    }

    private func load() {
        uiState.isLoading = true
        getSettlementAccountUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let account = (result as? AppResultSuccess<AnyObject>)?.value as? SettlementAccount

                self.uiState.isLoading = false
                if error == nil, let account {
                    self.uiState.bankName = account.bankName
                    // 조회는 마스킹된 번호만 준다 — 그대로 저장하면 실제 번호가 덮인다.
                    // 편집 필드는 비우고 마스킹 값은 안내로만 보여준다.
                    self.uiState.accountNumber = ""
                    self.uiState.maskedAccountNumber = account.maskedAccountNumber
                    self.uiState.holderName = account.holderName
                } else if let appError = (result as? AppResultFailure)?.error, !(appError is AppError.NotFound) {
                    // 미등록(404)은 빈 폼으로 시작한다 (M-12-3)
                    self.event.send(.showNotice(message: "계좌 정보를 불러오지 못했어요"))
                }
            }
        }
    }

    private func onSubmit() {
        let state = uiState

        if !state.canSubmit {
            return
        }
        uiState.isSubmitting = true

        let account = SettlementAccount(
            bankName: state.bankName,
            maskedAccountNumber: state.accountNumber,
            holderName: state.holderName
        )

        saveSettlementAccountUseCase.invoke(account: account) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isSubmitting = false
                if error == nil, result is AppResultSuccess<AnyObject> {
                    self.event.send(.saved)
                } else {
                    let appError = (result as? AppResultFailure)?.error

                    self.event.send(.showNotice(message: self.saveFailMessage(appError)))
                }
            }
        }
    }

    // 서버 code 기반 문구 분기 (규칙 §10)
    private func saveFailMessage(_ error: AppError?) -> String {
        if let validation = error as? AppError.ValidationFailed {
            return validation.serverMessage ?? "계좌 정보를 확인해 주세요"
        } else if error is AppError.NetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return "계좌를 저장하지 못했어요. 다시 시도해 주세요"
        }
    }

    func action(_ action: SettlementAccountAction) {
        switch action {
        case .enter:
            onEnter()
        case let .changeBankName(text):
            uiState.bankName = text
        case let .changeAccountNumber(text):
            uiState.accountNumber = String(text.filter { $0.isNumber || $0 == "-" }.prefix(20))
        case let .changeHolderName(text):
            uiState.holderName = text
        case .submit:
            onSubmit()
        }
    }

    init(
        getSettlementAccountUseCase: GetSettlementAccountUseCase,
        saveSettlementAccountUseCase: SaveSettlementAccountUseCase
    ) {
        self.getSettlementAccountUseCase = getSettlementAccountUseCase
        self.saveSettlementAccountUseCase = saveSettlementAccountUseCase
    }
}
