enum PlayAction {
    case enter(pin: String)
    case selectChoice(index: Int)
    case changeEssayAnswer(text: String)
    case clickSubmit
    case confirmLeave
    case clickViewReport
}
