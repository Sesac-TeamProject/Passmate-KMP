package org.sesacteamproject.passmate.navigation

// 화면은 이 액션만 방출하고, 실제 이동은 플랫폼 셸(AppNavHost)이 수행한다
sealed interface NavigationAction {

    data object NavigateToHome : NavigationAction

    data object NavigateToSignIn : NavigationAction

    data object NavigateBack : NavigationAction
}
