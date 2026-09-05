import Combine
import Foundation
import Shared

// Compose EditProfileViewModel.kt 미러 — 계정 정보 변경 (M-12-1). 캐릭터는 M-12-7이 담당한다
final class EditProfileViewModel: ObservableObject {
    private let getMyProfileUseCase: GetMyProfileUseCase

    private let updateMyProfileUseCase: UpdateMyProfileUseCase

    @Published private(set) var uiState = EditProfileUiState()

    let event = PassthroughSubject<EditProfileEvent, Never>()

    private var hasEntered = false

    private func loadProfile() {
        uiState.isLoading = true
        uiState.hasLoadError = false
        getMyProfileUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let profile = (result as? AppResultSuccess<AnyObject>)?.value as? UserProfile

                self.uiState.isLoading = false
                if error == nil, let profile {
                    self.uiState.nickname = profile.nickname
                    self.uiState.email = profile.email
                    self.uiState.avatarId = profile.avatarId?.intValue
                } else {
                    self.uiState.hasLoadError = true
                }
            }
        }
    }

    private func onEnter() {
        if hasEntered {
            return
        }
        hasEntered = true

        loadProfile()
    }

    private func onSubmit() {
        let state = uiState

        if !state.canSubmit {
            return
        }
        uiState.isSubmitting = true
        // 캐릭터는 M-12-7이 담당한다 — nil이면 전송에서 생략돼(explicitNulls=false) 값이 보존된다
        updateMyProfileUseCase.invoke(nickname: state.nickname, avatarId: nil) { [weak self] result, error in
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
        if let validation = error as? AppError.ValidationFailed {
            return validation.serverMessage ?? "닉네임을 확인해 주세요"
        } else if error is AppError.NetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return "저장하지 못했어요. 다시 시도해 주세요"
        }
    }

    func action(_ action: EditProfileAction) {
        switch action {
        case .enter:
            onEnter()
        case .retry:
            loadProfile()
        case let .changeNickname(text):
            uiState.nickname = String(text.prefix(12))
        case .submit:
            onSubmit()
        }
    }

    init(getMyProfileUseCase: GetMyProfileUseCase, updateMyProfileUseCase: UpdateMyProfileUseCase) {
        self.getMyProfileUseCase = getMyProfileUseCase
        self.updateMyProfileUseCase = updateMyProfileUseCase
    }
}
