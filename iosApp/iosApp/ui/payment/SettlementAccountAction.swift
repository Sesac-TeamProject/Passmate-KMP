enum SettlementAccountAction {
    case enter
    case changeBankName(text: String)
    case changeAccountNumber(text: String)
    case changeHolderName(text: String)
    case submit
}
