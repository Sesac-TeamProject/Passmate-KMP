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
    // 회원 탈퇴 (M-12-12) — 마이 › 회원 탈퇴 행에서 진입
    case deleteAccount
    // 계정 정보 변경 (M-12-1) — 닉네임·이메일 + 캐릭터 바꾸기 링크
    case editProfile
    // 내 캐릭터 변경 (M-12-7) — M-12-1의 "캐릭터 바꾸기 →"에서 진입
    case characterEdit
    // 정산 계좌 등록 (M-12-3) — 마이 · 정산(M-T4) 양쪽에서 진입
    case settlementAccount
    // 결제 수단 관리 (M-12-8)
    case paymentMethod
    // 알림 설정 (M-12-10)
    case notificationSettings

    // 탭 루트에서 push되지만 시안이 하단 탭바를 유지하는 화면 (M-12-x 전부 · M-14 방 리포트).
    // Compose AppTab.barOwnerOf와 같은 규칙이다 — nil이면 탭바를 숨긴다
    var tabBarOwner: AppTab? {
        switch self {
        case .editProfile, .characterEdit, .settlementAccount, .coinCharge,
             .paymentMethod, .coinHistory, .notificationSettings, .deleteAccount:
            return .myInfo
        case .roomReport:
            return .hostedRooms
        default:
            return nil
        }
    }

    // 세션 플로우 엔트리(Join·Payment·Waiting·Play) — Result 진입 시 이것만 제거하고 탭 루트는 유지한다 (규칙 §2-1-2, 스펙 §1-5)
    var isSessionRoute: Bool {
        switch self {
        case .join, .payment, .waiting, .play: return true
        default: return false
        }
    }
}
