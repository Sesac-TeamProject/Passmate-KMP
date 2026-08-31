import Foundation

enum AppShellAction {
    case selectTab(AppTab)

    // SignIn 진입 시 항상 호출한다 — 목적지가 없으면 nil로 덮어써 이전 값을 무효화한다 (스펙 §0 stale 방지)
    case rememberPendingRoute(Route?)

    // 로그인 성공 — pendingRoute 유무로 목적지를 정한다
    case resumeAfterSignIn
}
