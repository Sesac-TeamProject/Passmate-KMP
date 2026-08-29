import Combine
import Foundation
import Shared

// Compose EditProfileViewModel.kt 미러 — 닉네임·기본 캐릭터 수정 (M-12-1·M-12-7)
final class EditProfileViewModel: ObservableObject {
    private let updateMyProfileUseCase: UpdateMyProfileUseCase

    @Published private(set) var uiState = EditProfileUiState()

    let event = PassthroughSubject<EditProfileEvent, Never>()

    private var hasEntered = false

    private func onEnter(nickname: String, avatarId: Int?) {
        if hasEntered {
            return
        }
        hasEntered = true
        uiState.nickname = nickname
        uiState.avatarId = avatarId
    }

    private func onSubmit() {
        let state = uiState

        if !state.canSubmit {
            return
        }
        uiState.isSubmitting = true
        updateMyProfileUseCase.invoke(
            nickname: state.nickname,
            avatarId: state.avatarId.map { KotlinInt(value: Int32($0)) }
        ) { [weak self] result, error in
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

    // 서버 code 기반 문구 분기 (규칙 §10) — 닉네임 최종 검증은 서버가 한다
    private func saveFailMessage(_ error: AppError?) -> String {
        if let validation = error as? AppErrorValidationFailed {
            return validation.serverMessage ?? "닉네임을 확인해 주세요"
        } else if error is AppErrorNetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return "저장하지 못했어요. 다시 시도해 주세요"
        }
    }

    func action(_ action: EditProfileAction) {
        switch action {
        case let .enter(nickname, avatarId):
            onEnter(nickname: nickname, avatarId: avatarId)
        case let .changeNickname(text):
            uiState.nickname = String(text.prefix(12))
        case let .selectAvatar(avatarId):
            uiState.avatarId = avatarId
        case .submit:
            onSubmit()
        }
    }

    init(updateMyProfileUseCase: UpdateMyProfileUseCase) {
        self.updateMyProfileUseCase = updateMyProfileUseCase
    }
}
