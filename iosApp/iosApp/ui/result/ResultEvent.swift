enum ResultEvent {
    case shareReport(summary: String)
    case navigateToSignup
    case showNotice(message: String)
    case ratingSubmitted(message: String)
}
