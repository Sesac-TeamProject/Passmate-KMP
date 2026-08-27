enum SignInAction {
    case clickGoogleSignIn
    case clickAppleSignIn
    case clickGuestEnter
    case receiveOAuthCallback(accessToken: String, refreshToken: String)
}
