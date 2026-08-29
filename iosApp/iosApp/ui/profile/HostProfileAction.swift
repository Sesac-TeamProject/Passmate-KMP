import Shared

enum HostProfileAction {
    case enter(hostId: Int64)
    case retry(hostId: Int64)
    case clickRoom(pin: String)
    case clickBlock
    case submitReport(reason: ReportReason)
}
