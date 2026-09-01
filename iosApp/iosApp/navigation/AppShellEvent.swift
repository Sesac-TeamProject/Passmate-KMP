import Foundation

enum AppShellEvent {
    case navigateToTab(AppTab)

    // 게스트가 로그인 필수 탭을 누름 — 화면을 열지 않고 SignIn으로 (결정 2)
    case requireSignIn

    // 로그인 성공 + pendingRoute 있음 — SignIn을 걷어내고 목적지로 복귀한다 (스펙 §4-0)
    case resumePendingRoute(Route)

    // 로그인 성공 + pendingRoute 없음 — 현행대로 홈으로
    case navigateToHome
}
