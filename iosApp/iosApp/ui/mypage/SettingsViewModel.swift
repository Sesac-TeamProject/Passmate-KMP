import Combine
import Foundation
import Shared

// Compose SettingsViewModel.kt 미러 — 내 정보 관리 허브 (M-12)
final class SettingsViewModel: ObservableObject {
    private let getMyProfileUseCase: GetMyProfileUseCase

    private let signOutUseCase: SignOutUseCase

    private let deleteAccountUseCase: DeleteAccountUseCase

    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = SettingsUiState()

    let event = PassthroughSubject<SettingsEvent, Never>()

    private var hasEntered = false

    private func onEnter() {
        if hasEntered {
            return
        }
        hasEntered = true
        // 회원 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if isSignedInUseCase.invoke() {
            load()
        } else {
            event.send(.requireSignIn)
        }
    }

    private func load() {
        uiState.isLoading = true
        uiState.loadFailed = false
        getMyProfileUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                let profile = (result as? AppResultSuccess<AnyObject>)?.value as? UserProfile

                if error == nil, let profile {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = false
                    self.uiState.profile = profile
                } else {
                    self.uiState.isLoading = false
                    self.uiState.loadFailed = true
                }
            }
        }
    }

    private func onClickEditProfile() {
        if let profile = uiState.profile {
            event.send(.openEditProfile(
                nickname: profile.nickname,
                avatarId: profile.avatarId.map { Int(truncating: $0) }
            ))
        }
    }

    private func onConfirmSignOut() {
        if uiState.isProcessing {
            return
        }
        uiState.isProcessing = true
        // 로컬 세션 정리는 shared가 항상 수행 — 실패 케이스 없음 (M-12-11)
        signOutUseCase.invoke { [weak self] _, _ in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isProcessing = false
                self.event.send(.signedOut)
            }
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
        if let conflict = error as? AppErrorConflict {
            return conflict.serverMessage ?? "정산 대기 금액이나 진행 중인 방이 있어 탈퇴할 수 없어요"
        } else if error is AppErrorNetworkError {
            return "네트워크 연결을 확인해 주세요"
        } else {
            return "탈퇴를 처리하지 못했어요. 다시 시도해 주세요"
        }
    }

    func action(_ action: SettingsAction) {
        switch action {
        case .enter:
            onEnter()
        case .retry:
            load()
        case .clickEditProfile:
            onClickEditProfile()
        case .clickPaymentMethod:
            event.send(.openPaymentMethod)
        case .clickNotifications:
            event.send(.openNotifications)
        case .clickCoinHistory:
            event.send(.openCoinHistory)
        case .confirmSignOut:
            onConfirmSignOut()
        case .confirmDeleteAccount:
            onConfirmDeleteAccount()
        case .profileUpdated:
            load()
            event.send(.showNotice(message: "내 정보를 저장했어요"))
        case let .notice(message):
            event.send(.showNotice(message: message))
        }
    }

    init(
        getMyProfileUseCase: GetMyProfileUseCase,
        signOutUseCase: SignOutUseCase,
        deleteAccountUseCase: DeleteAccountUseCase,
        isSignedInUseCase: IsSignedInUseCase
    ) {
        self.getMyProfileUseCase = getMyProfileUseCase
        self.signOutUseCase = signOutUseCase
        self.deleteAccountUseCase = deleteAccountUseCase
        self.isSignedInUseCase = isSignedInUseCase
    }
}
