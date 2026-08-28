import Shared

enum PlayEvent {
    case playVoiceHint(hint: VoiceHint)
    case roomClosed(message: String)
    case left
    case showNotice(message: String)
}
