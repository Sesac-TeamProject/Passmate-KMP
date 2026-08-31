import Combine
import Foundation
import Shared

// Compose AppShellViewModel.kt 미러 — 하단 탭 게스트 가드 + pendingRoute 보관 (규칙 §7·§8, 스펙 §2-2)
final class AppShellViewModel: ObservableObject {
    private let isSignedInUseCase: IsSignedInUseCase

    @Published private(set) var uiState = AppShellUiState()

    let event = PassthroughSubject<AppShellEvent, Never>()

    private func onSelectTab(_ tab: AppTab) {
        let isSignedIn = isSignedInUseCase.invoke()
        let isGuarded = tab.requiresSignIn && !isSignedIn

        uiState.isSignedIn = isSignedIn
        if isGuarded {
            uiState.pendingRoute = tab.route
            event.send(.requireSignIn)
        } else {
            event.send(.navigateToTab(tab))
        }
    }

    private func onRememberPendingRoute(_ pendingRoute: Route?) {
        uiState.pendingRoute = pendingRoute
    }

    private func onResumeAfterSignIn() {
        let pendingRoute = uiState.pendingRoute

        uiState.isSignedIn = isSignedInUseCase.invoke()
        uiState.pendingRoute = nil
        if let pendingRoute {
            event.send(.resumePendingRoute(pendingRoute))
        } else {
            event.send(.navigateToHome)
        }
    }

    func action(_ action: AppShellAction) {
        switch action {
        case let .selectTab(tab):
            onSelectTab(tab)
        case let .rememberPendingRoute(pendingRoute):
            onRememberPendingRoute(pendingRoute)
        case .resumeAfterSignIn:
            onResumeAfterSignIn()
        }
    }

    init(isSignedInUseCase: IsSignedInUseCase) {
        self.isSignedInUseCase = isSignedInUseCase
    }
}
