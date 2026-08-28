import Shared

enum PlayEvent {
    case playVoiceHint(hint: VoiceHint)
    case openResult(roomId: Int64)
    case openSignup
    case roomClosed(message: String)
    case left
    case showNotice(message: String)
}
