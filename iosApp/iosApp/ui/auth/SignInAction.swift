enum SignInAction {
    case clickGoogleSignIn
    case clickAppleSignIn
    case clickGuestEnter
    // 개발용 로그인 (POST /auth/dev-login) — 로컬 개발 서버에서만 노출된다
    case clickDevSignIn
    // 네이티브 SDK가 Google ID 토큰을 돌려줬다 — 서버 로그인은 여기서 시작한다
    case receiveGoogleIdToken(idToken: String)
    // 사용자가 구글 시트를 닫았다 — 실패가 아니므로 안내를 띄우지 않는다
    case cancelGoogleSignIn
    // 구글 SDK가 토큰을 주지 못했다 — 안내 문구는 ViewModel이 정한다 (규칙 §7)
    case failGoogleSignIn
    // 아래 셋은 iOS 전용이다 — Android·Desktop은 Apple 로그인을 열지 않아 Compose 미러에 짝이 없다
    // Apple이 identity token을 돌려줬다. authorizationCode는 탈퇴 시 연결 해제용, name은 첫 인증 때만 온다
    case receiveAppleIdToken(idToken: String, authorizationCode: String?, name: String?)
    // 사용자가 Apple 시트를 닫았다 — 실패가 아니므로 안내를 띄우지 않는다
    case cancelAppleSignIn
    // Apple 시트가 토큰을 주지 못했다
    case failAppleSignIn
}
