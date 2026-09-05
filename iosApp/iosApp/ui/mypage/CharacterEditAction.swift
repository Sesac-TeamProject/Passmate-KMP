// Compose CharacterEditAction.kt 미러 (M-12-7)
enum CharacterEditAction {
    case enter
    case retry
    case selectAvatar(avatarId: Int)
    case submit
}
