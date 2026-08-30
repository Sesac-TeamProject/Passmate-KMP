import Foundation

// 하단 4탭 (피그마 v6) — Compose AppTab.kt 미러. 라우트·라벨·로그인 필수 여부 동일 (규칙 §2-1-1)
enum AppTab: Hashable, CaseIterable {
    case home
    case hostedRooms
    case joinedRooms
    case myInfo

    var label: String {
        switch self {
        case .home: return "홈"
        case .hostedRooms: return "내가 만든 방"
        case .joinedRooms: return "참여한 방"
        case .myInfo: return "마이"
        }
    }

    var systemImage: String {
        switch self {
        case .home: return "house"
        case .hostedRooms: return "plus.square"
        case .joinedRooms: return "rectangle.stack"
        case .myInfo: return "person"
        }
    }

    var requiresSignIn: Bool {
        return self != .home
    }
}
