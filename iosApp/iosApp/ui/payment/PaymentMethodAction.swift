import Shared

enum PaymentMethodAction {
    case enter
    case select(method: PaymentMethod)
    case submit
}
