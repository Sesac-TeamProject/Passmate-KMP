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
}
