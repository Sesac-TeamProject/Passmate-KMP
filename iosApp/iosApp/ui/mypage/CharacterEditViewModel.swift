import Combine
import Foundation
import Shared

// Compose CharacterEditViewModel.kt 미러 — 내 캐릭터 변경 (M-12-7). 닉네임은 M-12-1이 담당한다
final class CharacterEditViewModel: ObservableObject {
    private let getMyProfileUseCase: GetMyProfileUseCase

    private let updateMyProfileUseCase: UpdateMyProfileUseCase

    @Published private(set) var uiState = CharacterEditUiState()

    let event = PassthroughSubject<CharacterEditEvent, Never>()

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
        // 닉네임은 M-12-1이 담당한다 — nil이면 전송에서 생략돼(explicitNulls=false) 값이 보존된다
        updateMyProfileUseCase.invoke(
            nickname: nil,
            avatarId: state.avatarId.map { KotlinInt(value: Int32($0)) }
        ) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isSubmitting = false
                if error == nil, result is AppResultSuccess<AnyObject> {
                    self.event.send(.saved)
                } else {
                    self.event.send(.showNotice(message: self.saveFailMessage(result)))
                }
            }
        }
    }

    // 서버 code 기반 문구 분기 (규칙 §10)
    private func saveFailMessage(_ result: Any?) -> String {
        let error = (result as? AppResultFailure)?.error

        if error is AppError.NetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return "저장하지 못했어요. 다시 시도해 주세요"
        }
    }

    func action(_ action: CharacterEditAction) {
        switch action {
        case .enter:
            onEnter()
        case .retry:
            loadProfile()
        case let .selectAvatar(avatarId):
            uiState.avatarId = avatarId
        case .submit:
            onSubmit()
        }
    }

    init(getMyProfileUseCase: GetMyProfileUseCase, updateMyProfileUseCase: UpdateMyProfileUseCase) {
        self.getMyProfileUseCase = getMyProfileUseCase
        self.updateMyProfileUseCase = updateMyProfileUseCase
    }
}
