import Foundation

enum AppShellEvent {
    case navigateToTab(AppTab)

    // 게스트가 로그인 필수 탭을 누름 — 화면을 열지 않고 SignIn으로 (결정 2)
    case requireSignIn
}
