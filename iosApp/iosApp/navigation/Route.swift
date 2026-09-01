// 공통 라우트 규격 (규칙 §2-1-1) — 이름·인자는 3플랫폼 동일 유지. Compose Route.kt와 1:1
enum Route: Hashable {
    case home
    case roomList
    case signIn
    case join(pin: String?)
    case waiting(pin: String)
    case play(pin: String)
    case result(roomId: Int64)
    case myInfo
    case joinedRooms
    case reputation
    case hostedRooms
    case roomReport(roomId: Int64)
    case sessionControl(roomId: Int64, pin: String)
    case earnings
    case payment(pin: String)
    case coinHistory
    case coinCharge
    case settings
    // 회원 탈퇴 (M-12-12) — 설정에서 진입
    case deleteAccount

    // 세션 플로우 엔트리(Join·Payment·Waiting·Play) — Result 진입 시 이것만 제거하고 탭 루트는 유지한다 (규칙 §2-1-2, 스펙 §1-5)
    var isSessionRoute: Bool {
        switch self {
        case .join, .payment, .waiting, .play: return true
        default: return false
        }
    }
}
