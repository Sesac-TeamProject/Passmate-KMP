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

    // 시안 v6 nav/4탭의 icon/* 과 1:1 — Compose PassmateBottomTabBar.iconFor와 같은 키 (규칙 §11-3·§14)
    var icon: PassmateIcons {
        switch self {
        case .home: return .home
        case .hostedRooms: return .plusSquare
        case .joinedRooms: return .doorOpen
        case .myInfo: return .user
        }
    }

    var requiresSignIn: Bool {
        return self != .home
    }

    // 탭 루트에 대응하는 Route — pendingRoute로 탭 복귀를 표현할 때 쓴다 (스펙 §2-3)
    var route: Route {
        switch self {
        case .home: return .home
        case .hostedRooms: return .hostedRooms
        case .joinedRooms: return .joinedRooms
        case .myInfo: return .myInfo
        }
    }
}
