import Combine
import Foundation
import Shared

// Compose AppShellViewModel.kt 미러 — 하단 탭 게스트 가드 (규칙 §8, 결정 2)
final class AppShellViewModel: ObservableObject {
    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = AppShellUiState()

    let event = PassthroughSubject<AppShellEvent, Never>()

    private func onSelectTab(_ tab: AppTab) {
        let isSignedIn = isSignedInUseCase.invoke()

        uiState.isSignedIn = isSignedIn
        if tab.requiresSignIn && !isSignedIn {
            event.send(.requireSignIn)
        } else {
            event.send(.navigateToTab(tab))
        }
    }

    func action(_ action: AppShellAction) {
        switch action {
        case let .selectTab(tab):
            onSelectTab(tab)
        }
    }

    init(isSignedInUseCase: IsSignedInUseCase) {
        self.isSignedInUseCase = isSignedInUseCase
    }
}
