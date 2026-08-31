import Combine
import Foundation
import Shared

// Compose SettingsViewModel.kt 미러 — 회원 탈퇴 진입점만 둔다. 탈퇴 자체는 DeleteAccountView(M-12-12)가 맡는다
final class SettingsViewModel: ObservableObject {
    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = SettingsUiState()

    let event = PassthroughSubject<SettingsEvent, Never>()

    private func onEnter() {
        // 회원 전용 가드 — 서버 검증이 최종 권위 (규칙 §8)
        if !isSignedInUseCase.invoke() {
            event.send(.requireSignIn)
        }
    }

    func action(_ action: SettingsAction) {
        switch action {
        case .enter:
            onEnter()
        }
    }

    init(isSignedInUseCase: IsSignedInUseCase) {
        self.isSignedInUseCase = isSignedInUseCase
    }
}
