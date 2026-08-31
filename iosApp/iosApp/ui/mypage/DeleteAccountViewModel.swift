import Combine
import Foundation
import Shared

// Compose DeleteAccountViewModel.kt 미러 — 삭제 대상 안내에 실제 보유 코인을 보여주고,
// 확인 체크 후에만 탈퇴를 실행한다. 정산 미지급분·진행 중 방은 서버가 409로 막는다 (규칙 §8)
final class DeleteAccountViewModel: ObservableObject {
    private let getMyCoinsUseCase: GetMyCoinsUseCase

    private let deleteAccountUseCase: DeleteAccountUseCase

    @Published private(set) var uiState = DeleteAccountUiState()

    let event = PassthroughSubject<DeleteAccountEvent, Never>()

    private func load() {
        uiState.isLoading = true
        getMyCoinsUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isLoading = false
                if let coins = (result as? AppResultSuccess<AnyObject>)?.value as? CoinBalance {
                    self.uiState.coins = Int(coins.balance)
                }
            }
        }
    }

    private func onClickDelete() {
        if !uiState.canDelete {
            return
        }
        uiState.isProcessing = true
        deleteAccountUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isProcessing = false
                if error == nil, result is AppResultSuccess<AnyObject> {
                    self.event.send(.deleted)
                } else {
                    let appError = (result as? AppResultFailure)?.error
                    self.event.send(.showNotice(message: self.deleteFailMessage(appError)))
                }
            }
        }
    }

    // 서버 code 기반 문구 분기 (규칙 §10) — 409=정산 미지급분·진행 중 방 거부
    private func deleteFailMessage(_ error: AppError?) -> String {
        if let conflict = error as? AppError.Conflict {
            return conflict.serverMessage ?? "정산 대기 금액이나 진행 중인 방이 있어 탈퇴할 수 없어요"
        } else if error is AppError.NetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return "탈퇴를 처리하지 못했어요. 다시 시도해 주세요"
        }
    }

    func action(_ action: DeleteAccountAction) {
        switch action {
        case .enter:
            load()
        case .toggleConfirm:
            uiState.isConfirmed.toggle()
        case .clickDelete:
            onClickDelete()
        }
    }

    init(getMyCoinsUseCase: GetMyCoinsUseCase, deleteAccountUseCase: DeleteAccountUseCase) {
        self.getMyCoinsUseCase = getMyCoinsUseCase
        self.deleteAccountUseCase = deleteAccountUseCase
    }
}
