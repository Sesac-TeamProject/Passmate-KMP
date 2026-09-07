enum SignInEvent {
    // 플랫폼 구글 로그인 시트를 띄워 달라는 요청 — ID 토큰은 액션으로 되돌아온다
    case requestGoogleSignIn
    case signInCompleted
    case guestEnterRequested
    case showNotice(message: String)
}
