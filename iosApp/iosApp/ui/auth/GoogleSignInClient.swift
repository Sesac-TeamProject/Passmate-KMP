import Foundation
import GoogleSignIn
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
        guard clientId != nil, let presenter = presentingViewController else {
            print("Passmate/GoogleSignIn: GIDClientID가 비어 있거나 표시할 화면을 찾지 못했다")
            completion(.failed)
            return
        }

        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            completion(outcome(result: result, error: error))
        }
    }
}
