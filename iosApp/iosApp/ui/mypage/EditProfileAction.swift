enum EditProfileAction {
    case enter(nickname: String, avatarId: Int?)
    case changeNickname(text: String)
    case selectAvatar(avatarId: Int)
    case submit
}
