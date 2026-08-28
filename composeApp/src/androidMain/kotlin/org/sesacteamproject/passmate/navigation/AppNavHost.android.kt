package org.sesacteamproject.passmate.navigation

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import org.sesacteamproject.passmate.ui.auth.SignInScreen
import org.sesacteamproject.passmate.ui.home.HomeScreen
import org.sesacteamproject.passmate.ui.join.JoinScreen
import org.sesacteamproject.passmate.ui.play.PlayScreen
import org.sesacteamproject.passmate.ui.waiting.WaitingScreen

// OAuth 콜백 딥링크 — 백엔드가 ?client=mobile 인가 완료 시 이 URI로 리다이렉트한다 (contracts §Auth)
private const val OAUTH_CALLBACK_DEEP_LINK =
    "passmate://oauth/callback?accessToken={accessToken}&refreshToken={refreshToken}"

private fun Context.findComponentActivity(): ComponentActivity? {
    var current: Context? = this

    while (current is ContextWrapper) {
        if (current is ComponentActivity) {
            return current
        }
        current = current.baseContext
    }
    return null
}

private fun NavHostController.handleNavigationAction(action: NavigationAction) {
    when (action) {
        is NavigationAction.NavigateToHome -> navigate(Route.Home.route) {
            popUpTo(Route.Home.route) { inclusive = true }
            launchSingleTop = true
        }
        is NavigationAction.NavigateToSignIn -> navigate(Route.SignIn.route)
        is NavigationAction.NavigateToJoin -> {
            if (action.pin != null) {
                navigate("join?pin=${action.pin}")
            } else {
                navigate("join")
            }
        }
        is NavigationAction.NavigateToWaiting -> navigate("waiting/${action.pin}")
        is NavigationAction.NavigateToPlay -> navigate("play/${action.pin}")
        is NavigationAction.NavigateBack -> popBackStack()
    }
}

@Composable
actual fun AppNavHost() {
    val navController = rememberNavController()
    val activity = LocalContext.current.findComponentActivity()

    DisposableEffect(activity, navController) {
        val listener = Consumer<Intent> { intent -> navController.handleDeepLink(intent) }

        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }
    NavHost(
        navController = navController,
        startDestination = Route.Home.route
    ) {
        composable(Route.Home.route) {
            HomeScreen(onNavigate = navController::handleNavigationAction)
        }
        composable(
            route = "${Route.SignIn.route}?accessToken={accessToken}&refreshToken={refreshToken}",
            arguments = listOf(
                navArgument("accessToken") {
                    nullable = true
                    defaultValue = null
                },
                navArgument("refreshToken") {
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(navDeepLink { uriPattern = OAUTH_CALLBACK_DEEP_LINK })
        ) { backStackEntry ->
            SignInScreen(
                oauthAccessToken = backStackEntry.arguments?.getString("accessToken"),
                oauthRefreshToken = backStackEntry.arguments?.getString("refreshToken"),
                onNavigate = navController::handleNavigationAction
            )
        }
        composable(
            route = Route.Join.route,
            arguments = listOf(
                navArgument("pin") {
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            JoinScreen(
                initialPin = backStackEntry.arguments?.getString("pin"),
                onNavigate = navController::handleNavigationAction
            )
        }
        composable(Route.Waiting.route) { backStackEntry ->
            WaitingScreen(
                pin = backStackEntry.arguments?.getString("pin").orEmpty(),
                onNavigate = navController::handleNavigationAction
            )
        }
        composable(Route.Play.route) { backStackEntry ->
            PlayScreen(
                pin = backStackEntry.arguments?.getString("pin").orEmpty(),
                onNavigate = navController::handleNavigationAction
            )
        }
    }
}
