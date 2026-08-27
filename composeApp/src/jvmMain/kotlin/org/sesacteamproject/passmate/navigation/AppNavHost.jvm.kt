package org.sesacteamproject.passmate.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import org.sesacteamproject.passmate.ui.auth.SignInScreen
import org.sesacteamproject.passmate.ui.home.HomeScreen

// Desktop은 상태 기반 라우트 상태머신 (규칙 §2-1). OAuth 딥링크 콜백은 데스크톱 미지원 — 모바일/웹 사용 안내
@Composable
actual fun AppNavHost() {
    val routeStack = remember { mutableStateListOf<Route>(Route.Home) }
    val currentRoute = routeStack.last()
    val onNavigate: (NavigationAction) -> Unit = { action ->
        when (action) {
            is NavigationAction.NavigateToHome -> {
                routeStack.clear()
                routeStack.add(Route.Home)
            }
            is NavigationAction.NavigateToSignIn -> routeStack.add(Route.SignIn)
            is NavigationAction.NavigateBack -> {
                if (routeStack.size > 1) {
                    routeStack.removeAt(routeStack.lastIndex)
                }
            }
        }
    }

    when (currentRoute) {
        is Route.SignIn -> SignInScreen(onNavigate = onNavigate)
        else -> HomeScreen(onNavigate = onNavigate)
    }
}
