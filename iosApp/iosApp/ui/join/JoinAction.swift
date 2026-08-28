enum JoinAction {
    case changePin(pin: String)
    case changeNickname(nickname: String)
    case selectAvatar(avatarId: Int)
    case clickScanQr
    case receiveQrResult(text: String?)
    case clickJoin
    case clickSignIn
}
