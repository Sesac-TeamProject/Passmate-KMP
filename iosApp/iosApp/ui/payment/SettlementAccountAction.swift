import Shared

enum SettlementAccountAction {
    case enter
    case selectBank(bank: Bank)
    case changeAccountNumber(text: String)
    case changeHolderName(text: String)
    case submit
}
