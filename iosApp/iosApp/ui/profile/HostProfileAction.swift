import Shared

enum HostProfileAction {
    case enter(hostId: Int64)
    case retry(hostId: Int64)
    // 프로필의 방 목록도 pin이 없다 — roomId로 받는다
    case clickRoom(roomId: Int64)
    case clickBlock
    case submitReport(reason: ReportReason)
}
