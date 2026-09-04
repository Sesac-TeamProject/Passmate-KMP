import Shared

enum PaymentAction {
    case start(pin: String)
    case changeNickname(nickname: String)
    case selectAvatar(avatarId: Int)
    case selectMethod(method: PaymentMethod)
    case clickPay
    case confirmCharge
    case dismissCoinShortage
    case receivePortOneResult(result: PortOneResult)
    case dismissError
    case retry
}
