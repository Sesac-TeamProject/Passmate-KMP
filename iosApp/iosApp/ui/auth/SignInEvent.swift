enum SignInEvent {
    case openSignInPage(url: String)
    case signInCompleted
    case guestEnterRequested
    case showNotice(message: String)
}
