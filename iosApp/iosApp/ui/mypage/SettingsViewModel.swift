import Combine
import Foundation
import Shared

// Compose SettingsViewModel.kt 미러 — 회원 탈퇴(M-12-12)만
final class SettingsViewModel: ObservableObject {
    private let deleteAccountUseCase: DeleteAccountUseCase

    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = SettingsUiState()

    let event = PassthroughSubject<SettingsEvent, Never>()

    private func onEnter() {
        // 회원 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if !isSignedInUseCase.invoke() {
            event.send(.requireSignIn)
        }
    }

    private func onConfirmDeleteAccount() {
        if uiState.isProcessing {
            return
        }
        uiState.isProcessing = true
        deleteAccountUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isProcessing = false
                if error == nil, result is AppResultSuccess<AnyObject> {
                    self.event.send(.accountDeleted)
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

    func action(_ action: SettingsAction) {
        switch action {
        case .enter:
            onEnter()
        case .confirmDeleteAccount:
            onConfirmDeleteAccount()
        }
    }

    init(deleteAccountUseCase: DeleteAccountUseCase, isSignedInUseCase: IsSignedInUseCase) {
        self.deleteAccountUseCase = deleteAccountUseCase
        self.isSignedInUseCase = isSignedInUseCase
    }
}
