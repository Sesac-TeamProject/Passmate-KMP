import Foundation
import GoogleSignIn
import Shared
import UIKit

// 구글 로그인 결과 — Compose의 rememberGoogleSignInLauncher 콜백 3종과 1:1이다
enum GoogleSignInOutcome {
    case idToken(String)
    case cancelled
    case failed
}

/// 구글 로그인 시트를 띄우고 ID 토큰만 돌려준다. 상태·문구 판단은 ViewModel이 한다(규칙 §7).
///
/// 클라이언트 ID는 Info.plist의 `GIDClientID`에서 SDK가 직접 읽는다. 값이 비어 있으면
/// SDK가 단언에 걸려 앱이 죽으므로, 먼저 확인하고 실패로 돌린다.
///
/// serverClientID(웹 클라이언트 ID)를 함께 줘야 ID 토큰의 `aud`가 서버가 검증하는 값이 된다.
/// 주지 않으면 aud가 iOS 클라이언트 ID로 나와 서버가 토큰을 거절한다 — 계정 선택까지는
/// 되고 로그인만 안 되는 증상이 이것이다. 안드로이드가 serverClientId를 넘기는 것과 짝이다.
enum GoogleSignInClient {

    private static var clientId: String? {
        let value = Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String

        return value?.isEmpty == false ? value : nil
    }

    private static var presentingViewController: UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var controller = scene?.keyWindow?.rootViewController

        while let presented = controller?.presentedViewController {
            controller = presented
        }
        return controller
    }

    private static func outcome(result: GIDSignInResult?, error: Error?) -> GoogleSignInOutcome {
        if let error = error as NSError?, error.code == GIDSignInError.canceled.rawValue {
            return .cancelled
        } else if let idToken = result?.user.idToken?.tokenString {
            return .idToken(idToken)
        } else {
            print("Passmate/GoogleSignIn: \(error?.localizedDescription ?? "ID 토큰이 비어 있다")")
            return .failed
        }
    }

    static func requestIdToken(completion: @escaping (GoogleSignInOutcome) -> Void) {
        guard let clientId, let presenter = presentingViewController else {
            print("Passmate/GoogleSignIn: GIDClientID가 비어 있거나 표시할 화면을 찾지 못했다")
            completion(.failed)
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: clientId,
            serverClientID: GoogleAuthConfig.shared.WEB_CLIENT_ID
        )
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            completion(outcome(result: result, error: error))
        }
    }
}
