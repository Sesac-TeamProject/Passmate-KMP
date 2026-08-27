import Combine
import Foundation
import Shared

final class SignInViewModel: ObservableObject {
    private let buildGoogleSignInUrlUseCase: BuildGoogleSignInUrlUseCase

    private let completeSignInUseCase: CompleteSignInUseCase

    @Published private(set) var uiState: SignInUiState

    let event = PassthroughSubject<SignInEvent, Never>()

    private func onClickGoogleSignIn() {
        let url = buildGoogleSignInUrlUseCase.invoke()

        event.send(.openSignInPage(url: url))
    }

    private func onClickAppleSignIn() {
        event.send(.showNotice(message: "Apple 로그인은 준비 중이에요"))
    }

    private func onClickGuestEnter() {
        event.send(.guestEnterRequested)
    }

    private func onReceiveOAuthCallback(accessToken: String, refreshToken: String) {
        if uiState.isSigningIn {
            return
        }
        uiState.isSigningIn = true
        completeSignInUseCase.invoke(accessToken: accessToken, refreshToken: refreshToken) { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isSigningIn = false
                if error == nil, result != nil, !(result is AppResultFailure) {
                    self.event.send(.signInCompleted)
                } else {
                    self.event.send(.showNotice(message: "로그인에 실패했어요. 다시 시도해 주세요"))
                }
            }
        }
    }

    func action(_ action: SignInAction) {
        switch action {
        case .clickGoogleSignIn:
            onClickGoogleSignIn()
        case .clickAppleSignIn:
            onClickAppleSignIn()
        case .clickGuestEnter:
            onClickGuestEnter()
        case let .receiveOAuthCallback(accessToken, refreshToken):
            onReceiveOAuthCallback(accessToken: accessToken, refreshToken: refreshToken)
        }
    }

    init(
        buildGoogleSignInUrlUseCase: BuildGoogleSignInUrlUseCase,
        completeSignInUseCase: CompleteSignInUseCase
    ) {
        self.buildGoogleSignInUrlUseCase = buildGoogleSignInUrlUseCase
        self.completeSignInUseCase = completeSignInUseCase
        self.uiState = SignInUiState()
    }
}
