enum SignInAction {
    case clickGoogleSignIn
    case clickAppleSignIn
    case clickGuestEnter
    // 개발용 로그인 (POST /auth/dev-login) — 로컬 개발 서버에서만 노출된다
    case clickDevSignIn
    case receiveOAuthCallback(accessToken: String, refreshToken: String)
}
