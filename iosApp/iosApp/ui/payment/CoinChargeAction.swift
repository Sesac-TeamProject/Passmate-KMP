import Shared

enum CoinChargeAction {
    case enter
    case retry
    case selectAmount(amount: Int)
    case selectMethod(method: PaymentMethod)
    // 주 CTA — 충전 요청 후 포트원 결제창을 띄운다
    case clickCharge
    case receivePortOneResult(result: PortOneResult)
    // 완료 화면(M-12-6)의 "확인"
    case clickConfirmDone
    case dismissError
}
