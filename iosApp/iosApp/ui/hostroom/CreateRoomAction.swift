enum CreateRoomAction {
    case enter
    case retrySets
    case changeTitle(title: String)
    case selectSet(setId: Int64)
    case selectPaid(isPaid: Bool)
    case changeEntryFee(text: String)
    case submit
}
