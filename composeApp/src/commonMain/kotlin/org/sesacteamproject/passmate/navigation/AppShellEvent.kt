package org.sesacteamproject.passmate.navigation

sealed interface AppShellEvent {
    data class NavigateToTab(val tab: AppTab) : AppShellEvent

    // 게스트가 로그인 필수 탭을 누름 — 화면을 열지 않고 SignIn으로 (결정 2)
    data object RequireSignIn : AppShellEvent

    // 로그인 성공 + pendingRoute 있음 — SignIn을 걷어내고 목적지로 복귀한다 (스펙 §4-0)
    data class ResumePendingRoute(val pendingRoute: NavigationAction) : AppShellEvent

    // 로그인 성공 + pendingRoute 없음 — 현행대로 홈으로
    data object NavigateToHome : AppShellEvent
}
