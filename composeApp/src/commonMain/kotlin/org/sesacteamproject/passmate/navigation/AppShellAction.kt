package org.sesacteamproject.passmate.navigation

sealed interface AppShellAction {
    data class SelectTab(val tab: AppTab) : AppShellAction

    // SignIn 진입 시 항상 호출한다 — 목적지가 없으면 null로 덮어써 이전 값을 무효화한다 (스펙 §0 stale 방지)
    data class RememberPendingRoute(val pendingRoute: NavigationAction?) : AppShellAction

    // 로그인 성공 — pendingRoute 유무로 목적지를 정한다
    data object ResumeAfterSignIn : AppShellAction
}
