enum ResultAction {
    case enter(roomId: Int64)
    case selectQuestion(questionNo: Int)
    case clickExport
    case retry
}
