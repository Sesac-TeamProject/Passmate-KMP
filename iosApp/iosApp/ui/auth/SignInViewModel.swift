import Combine
import Foundation
import Shared

final class SignInViewModel: ObservableObject {
    private let buildGoogleSignInUrlUseCase: BuildGoogleSignInUrlUseCase

    private let completeSignInUseCase: CompleteSignInUseCase

    private let completeGuestClaimUseCase: CompleteGuestClaimUseCase

    private let devSignInUseCase: DevSignInUseCase

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

    // 개발용 로그인 — 서버가 바로 토큰 쌍을 주므로 브라우저 왕복이 없다
    private func onClickDevSignIn() {
        if uiState.isSigningIn {
            return
        }
        uiState.isSigningIn = true
        devSignInUseCase.invoke { [weak self] result, error in
            DispatchQueue.main.async {
                guard let self else { return }
                self.uiState.isSigningIn = false
                if error == nil, result != nil, !(result is AppResultFailure) {
                    self.claimPendingGuestRecord()
                    self.event.send(.signInCompleted)
                } else {
                    self.event.send(.showNotice(message: "개발 로그인에 실패했어요. 로컬 백엔드가 떠 있는지 확인해 주세요"))
                }
            }
        }
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
                    self.claimPendingGuestRecord()
                    self.event.send(.signInCompleted)
                } else {
                    self.event.send(.showNotice(message: "로그인에 실패했어요. 다시 시도해 주세요"))
                }
            }
        }
    }

    // 가입 유도로 진입했다면 대기 중인 게스트 기록을 연동한다 (FR-036)
    private func claimPendingGuestRecord() {
        completeGuestClaimUseCase.invoke { [weak self] result, _ in
            DispatchQueue.main.async {
                guard let self else { return }
                if let failure = result as? AppResultFailure {
                    if failure.error is AppError.Gone {
                        self.event.send(.showNotice(message: "기록 보관 기간(7일)이 지나 저장하지 못했어요"))
                    } else {
                        self.event.send(.showNotice(message: "기록을 계정에 저장하지 못했어요"))
                    }
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
        case .clickDevSignIn:
            onClickDevSignIn()
        case let .receiveOAuthCallback(accessToken, refreshToken):
            onReceiveOAuthCallback(accessToken: accessToken, refreshToken: refreshToken)
        }
    }

    init(
        buildGoogleSignInUrlUseCase: BuildGoogleSignInUrlUseCase,
        completeSignInUseCase: CompleteSignInUseCase,
        completeGuestClaimUseCase: CompleteGuestClaimUseCase,
        devSignInUseCase: DevSignInUseCase,
        isDevSignInAvailableUseCase: IsDevSignInAvailableUseCase
    ) {
        self.buildGoogleSignInUrlUseCase = buildGoogleSignInUrlUseCase
        self.completeSignInUseCase = completeSignInUseCase
        self.completeGuestClaimUseCase = completeGuestClaimUseCase
        self.devSignInUseCase = devSignInUseCase
        self.uiState = SignInUiState(
            isSigningIn: false,
            isDevSignInAvailable: isDevSignInAvailableUseCase.invoke()
        )
    }
}
