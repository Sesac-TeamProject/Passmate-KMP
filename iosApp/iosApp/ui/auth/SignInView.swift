import AuthenticationServices
import SwiftUI
import Shared

// Figma "UI 디자인 v6" M-Login(349:9040) 미러 — 민트 히어로 + 하단 로그인 시트
struct SignInView: View {
    var onSignedIn: () -> Void = {}

    var onGuestEnter: () -> Void = {}

    @StateObject private var viewModel = SignInViewModel(
        buildGoogleSignInUrlUseCase: KoinHelper.shared.buildGoogleSignInUrlUseCase(),
        completeSignInUseCase: KoinHelper.shared.completeSignInUseCase(),
        completeGuestClaimUseCase: KoinHelper.shared.completeGuestClaimUseCase(),
        devSignInUseCase: KoinHelper.shared.devSignInUseCase(),
        isDevSignInAvailableUseCase: KoinHelper.shared.isDevSignInAvailableUseCase()
    )

    @State private var authSession: ASWebAuthenticationSession?

    @State private var noticeMessage: String?

    private let authSessionCoordinator = AuthSessionCoordinator()

    private func handleAuthCallback(callbackUrl: URL?) {
        let components = callbackUrl.flatMap { URLComponents(url: $0, resolvingAgainstBaseURL: false) }
        let accessToken = components?.queryItems?.first(where: { $0.name == "accessToken" })?.value
        let refreshToken = components?.queryItems?.first(where: { $0.name == "refreshToken" })?.value

        if let accessToken, let refreshToken {
            viewModel.action(.receiveOAuthCallback(accessToken: accessToken, refreshToken: refreshToken))
        } else {
            noticeMessage = "로그인에 실패했어요. 다시 시도해 주세요"
        }
    }

    private func startAuthSession(url: String) {
        guard let authUrl = URL(string: url) else { return }
        let session = ASWebAuthenticationSession(url: authUrl, callbackURLScheme: "passmate") { callbackUrl, error in
            if error == nil {
                handleAuthCallback(callbackUrl: callbackUrl)
            }
        }

        session.presentationContextProvider = authSessionCoordinator
        authSession = session
        session.start()
    }

    var body: some View {
        SignInContentView(
            uiState: viewModel.uiState,
            onAction: { viewModel.action($0) }
        )
        .onReceive(viewModel.event) { event in
            switch event {
            case let .openSignInPage(url):
                startAuthSession(url: url)
            case .signInCompleted:
                onSignedIn()
            case .guestEnterRequested:
                onGuestEnter()
            case let .showNotice(message):
                noticeMessage = message
            }
        }
        .overlay(alignment: .bottom) {
            if let noticeMessage {
                NoticeToast(message: noticeMessage)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
                            self.noticeMessage = nil
                        }
                    }
            }
        }
    }
}

private struct SignInContentView: View {
    let uiState: SignInUiState

    let onAction: (SignInAction) -> Void

    var body: some View {
        VStack(spacing: 0) {
            heroSection
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            sheetSection
        }
        .background(PassmateColors.backgroundMint.ignoresSafeArea())
    }

    private var heroSection: some View {
        VStack(spacing: 14) {
            PassyMascotView()
                .frame(width: 120, height: 132)
            HStack(spacing: 8) {
                // 시안 확정 로고 락업(가로형 국문) — 마크는 정사각 32pt
                Image("BrandMark")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 32, height: 32)
                Text("패스메이트")
                    .font(.system(size: 28, weight: .bold))
                    .kerning(-0.56)
                    .foregroundColor(PassmateColors.textPrimary)
            }
            Text("혼자 시작한 공부,\n함께하는 합격까지.")
                .font(.system(size: 14))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textSecondary)
                .multilineTextAlignment(.center)
        }
        .padding(.top, 96)
        .padding(.horizontal, 28)
        .padding(.bottom, 36)
    }

    private var sheetSection: some View {
        VStack(spacing: 12) {
            googleButton
            appleButton
            orDivider
            guestButton
            if uiState.isDevSignInAvailable {
                devSignInButton
            }
            Text("계속하면 이용약관과 개인정보 처리방침에 동의한 것으로 봅니다\n선생님·학생 공용 계정 · 게스트 기록은 세션 후 사라져요")
                .font(.system(size: 12))
                .kerning(-0.24)
                .foregroundColor(PassmateColors.textTertiary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
        }
        .padding(.top, 28)
        .padding(.horizontal, 24)
        .padding(.bottom, 36)
        .background(
            PassmateColors.surface
                .clipShape(RoundedCorner(radius: 28, corners: [.topLeft, .topRight]))
                .ignoresSafeArea(edges: .bottom)
        )
    }

    private var googleButton: some View {
        Button {
            onAction(.clickGoogleSignIn)
        } label: {
            HStack(spacing: 10) {
                if uiState.isSigningIn {
                    ProgressView()
                        .frame(width: 24, height: 24)
                } else {
                    Image("GoogleSignIn")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 24, height: 24)
                }
                Text("Google로 계속하기")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.textPrimary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(PassmateColors.surface)
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(PassmateColors.border, lineWidth: 1))
            .cornerRadius(14)
        }
        .disabled(uiState.isSigningIn)
    }

    private var appleButton: some View {
        Button {
            onAction(.clickAppleSignIn)
        } label: {
            HStack(spacing: 10) {
                if uiState.isSigningIn {
                    ProgressView()
                        .tint(PassmateColors.surface)
                        .frame(width: 20, height: 20)
                } else {
                    Image("AppleSignIn")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                }
                Text("Apple로 계속하기")
                    .font(.system(size: 14, weight: .medium))
                    .kerning(-0.28)
                    .foregroundColor(PassmateColors.surface)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(PassmateColors.brandAppleBlack)
            .cornerRadius(14)
        }
        .disabled(uiState.isSigningIn)
    }

    private var orDivider: some View {
        HStack(spacing: 10) {
            Rectangle()
                .fill(PassmateColors.border)
                .frame(height: 1)
            Text("또는")
                .font(.system(size: 12))
                .kerning(-0.24)
                .foregroundColor(PassmateColors.textTertiary)
            Rectangle()
                .fill(PassmateColors.border)
                .frame(height: 1)
        }
    }

    // 로컬 개발 서버에서만 그려진다 — 운영 URL에서는 uiState.isDevSignInAvailable이 false다
    private var devSignInButton: some View {
        Button {
            onAction(.clickDevSignIn)
        } label: {
            Text("개발용 로그인 (로컬 서버)")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.textTertiary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(PassmateColors.surface)
                .cornerRadius(14)
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(PassmateColors.border, lineWidth: 1)
                )
        }
        .disabled(uiState.isSigningIn)
    }

    private var guestButton: some View {
        Button {
            onAction(.clickGuestEnter)
        } label: {
            Text("PIN으로 바로 입장 (게스트)")
                .font(.system(size: 14, weight: .medium))
                .kerning(-0.28)
                .foregroundColor(PassmateColors.primaryDeep)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(PassmateColors.backgroundMint)
                .cornerRadius(14)
        }
        .disabled(uiState.isSigningIn)
    }
}

private struct NoticeToast: View {
    let message: String

    var body: some View {
        Text(message)
            .font(.system(size: 13))
            .foregroundColor(PassmateColors.surface)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(PassmateColors.textPrimary.opacity(0.9))
            .cornerRadius(10)
            .padding(.bottom, 16)
    }
}

private struct RoundedCorner: Shape {
    let radius: CGFloat

    let corners: UIRectCorner

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

private final class AuthSessionCoordinator: NSObject, ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        let window = UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first

        return window ?? ASPresentationAnchor()
    }
}

// MARK: - 프리뷰 (Figma 시안 비교용, Koin 미초기화 상태에서도 안전한 콘텐츠 뷰 기반)

#Preview("기본") {
    SignInContentView(
        uiState: SignInUiState(),
        onAction: { _ in }
    )
}

#Preview("로그인 중 (isSigningIn)") {
    SignInContentView(
        uiState: SignInUiState(isSigningIn: true),
        onAction: { _ in }
    )
}
