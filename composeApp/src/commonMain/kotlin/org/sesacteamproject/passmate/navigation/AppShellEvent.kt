package org.sesacteamproject.passmate.navigation

sealed interface AppShellEvent {
    data class NavigateToTab(val tab: AppTab) : AppShellEvent

    // 게스트가 로그인 필수 탭을 누름 — 화면을 열지 않고 SignIn으로 (결정 2)
    data object RequireSignIn : AppShellEvent
}
