import AuthenticationServices
import Foundation
import UIKit

// 애플 로그인 결과 — GoogleSignInOutcome과 같은 3갈래다.
// authorizationCode는 서버가 refresh token으로 바꿔 두었다가 탈퇴 때 Apple 연결을 끊는 데 쓴다(5분 안에 1회용).
// name은 Apple이 첫 인증 때만 주는 이름이다. 두 번째 로그인부터는 nil이다
enum AppleSignInOutcome {
    case idToken(String, authorizationCode: String?, name: String?)
    case cancelled
    case failed
}

/// Sign in with Apple 시트를 띄우고 identity token(과 첫 인증 때의 이름)만 돌려준다.
/// 상태·문구 판단은 ViewModel이 한다(규칙 §7).
///
/// 서버는 토큰의 aud가 앱 번들 ID인지 확인한다 — 번들 ID를 바꾸면 백엔드
/// `APPLE_CLIENT_IDS`에도 같이 넣어야 한다. 타깃에 "Sign in with Apple" 기능
/// (iosApp.entitlements)이 없으면 시트가 뜨지 않고 실패(1000)로 끝난다.
final class AppleSignInClient: NSObject {

    // 시트가 떠 있는 동안 델리게이트를 붙잡아 둔다 — ASAuthorizationController는 delegate를 약하게 잡는다
    private static var current: AppleSignInClient?

    private let completion: (AppleSignInOutcome) -> Void

    private init(completion: @escaping (AppleSignInOutcome) -> Void) {
        self.completion = completion
    }

    private static var presentationWindow: UIWindow? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }

        return scene?.windows.first { $0.isKeyWindow } ?? scene?.windows.first
    }

    // Apple이 준 이름 조각을 기기 로케일 순서로 잇는다 (한국어면 성+이름)
    private static func displayName(_ components: PersonNameComponents?) -> String? {
        let formatted = components.map {
            PersonNameComponentsFormatter.localizedString(from: $0, style: .default)
        }
        let trimmed = formatted?.trimmingCharacters(in: .whitespacesAndNewlines)

        return trimmed?.isEmpty == false ? trimmed : nil
    }

    private func finish(_ outcome: AppleSignInOutcome) {
        DispatchQueue.main.async {
            self.completion(outcome)
            AppleSignInClient.current = nil
        }
    }

    static func requestIdToken(completion: @escaping (AppleSignInOutcome) -> Void) {
        let client = AppleSignInClient(completion: completion)
        let request = ASAuthorizationAppleIDProvider().createRequest()
        let controller = ASAuthorizationController(authorizationRequests: [request])

        request.requestedScopes = [.fullName, .email]
        controller.delegate = client
        controller.presentationContextProvider = client
        current = client
        controller.performRequests()
    }
}

extension AppleSignInClient: ASAuthorizationControllerDelegate {

    func authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization authorization: ASAuthorization
    ) {
        let credential = authorization.credential as? ASAuthorizationAppleIDCredential
        let idToken = credential?.identityToken.flatMap { String(data: $0, encoding: .utf8) }
        let authorizationCode = credential?.authorizationCode.flatMap { String(data: $0, encoding: .utf8) }
        let name = AppleSignInClient.displayName(credential?.fullName)

        if let idToken {
            finish(.idToken(idToken, authorizationCode: authorizationCode, name: name))
        } else {
            print("Passmate/AppleSignIn: identity token이 비어 있다")
            finish(.failed)
        }
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        if (error as? ASAuthorizationError)?.code == .canceled {
            finish(.cancelled)
        } else {
            print("Passmate/AppleSignIn: \(error.localizedDescription)")
            finish(.failed)
        }
    }
}

extension AppleSignInClient: ASAuthorizationControllerPresentationContextProviding {

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        AppleSignInClient.presentationWindow ?? ASPresentationAnchor()
    }
}
