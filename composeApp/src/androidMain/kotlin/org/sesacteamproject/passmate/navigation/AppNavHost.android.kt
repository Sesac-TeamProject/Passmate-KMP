package org.sesacteamproject.passmate.navigation

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import org.sesacteamproject.passmate.component.PassmateBottomTabBar
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.ui.auth.SignInScreen
import org.sesacteamproject.passmate.ui.home.RoomListScreen
import org.sesacteamproject.passmate.ui.join.JoinScreen
import org.sesacteamproject.passmate.ui.hostroom.HostedRoomsScreen
import org.sesacteamproject.passmate.ui.hostroom.RoomReportScreen
import org.sesacteamproject.passmate.ui.hostroom.SessionControlScreen
import org.sesacteamproject.passmate.ui.mypage.JoinedRoomsScreen
import org.sesacteamproject.passmate.ui.mypage.MyInfoScreen
import org.sesacteamproject.passmate.ui.mypage.ReputationScreen
import org.sesacteamproject.passmate.ui.mypage.SettingsScreen
import org.sesacteamproject.passmate.ui.payment.CoinHistoryScreen
import org.sesacteamproject.passmate.ui.payment.EarningsScreen
import org.sesacteamproject.passmate.ui.payment.PaymentScreen
import org.sesacteamproject.passmate.ui.play.PlayScreen
import org.sesacteamproject.passmate.ui.result.ResultScreen
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

// 하단 탭 전환 — 홈 루트 위의 화면을 걷어내고 탭 루트를 올린다. 플랫 그래프라 saveState/restoreState는 쓰지 않는다
// (쓰면 팝된 화면(SignIn 등)이 홈 탭 복귀 시 되살아난다). 탭별 백스택 보존은 범위 밖 (스펙 §1-4·§9)
private fun NavHostController.navigateToTab(tab: AppTab) {
    navigate(tab.route) {
        popUpTo(Route.Home.route)
        launchSingleTop = true
    }
}

// 홈으로 복귀 — 홈 루트(JoinScreen)를 재생성해 로그인/로그아웃 후 세션 상태를 다시 읽게 한다 (규칙 §8)
private fun NavHostController.navigateHome() {
    navigate(Route.Home.route) {
        popUpTo(Route.Home.route) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.handleNavigationAction(action: NavigationAction) {
    when (action) {
        is NavigationAction.NavigateToHome -> navigateHome()
        is NavigationAction.NavigateToTab -> navigateToTab(action.tab)
        is NavigationAction.NavigateToRoomList -> navigate(Route.RoomList.route)
        is NavigationAction.NavigateToSignIn -> navigate(Route.SignIn.route)
        is NavigationAction.NavigateToJoin -> {
            // 홈 탭이 곧 입장 폼 — pin 없는 Join은 홈 탭으로 (스펙 §1-1)
            if (action.pin != null) {
                navigate("join?pin=${action.pin}")
            } else {
                navigateHome()
            }
        }
        is NavigationAction.NavigateToWaiting -> navigate("waiting/${action.pin}")
        is NavigationAction.NavigateToPlay -> navigate("play/${action.pin}")
        is NavigationAction.NavigateToResult -> {
            // 세션 플로우 엔트리(Waiting·Play·Payment·Join)만 제거하고 탭 루트는 유지한다 (규칙 §2-1-2, 스펙 §1-5)
            val hadSession = popBackStack(Route.Waiting.route, inclusive = true)

            if (hadSession) {
                popBackStack(Route.Payment.route, inclusive = true)
                popBackStack(Route.Join.route, inclusive = true)
            }
            navigate("result/${action.roomId}")
        }
        is NavigationAction.NavigateToPayment -> navigate("payment/${action.pin}")
        is NavigationAction.NavigateToMyInfo -> navigate(Route.MyInfo.route)
        is NavigationAction.NavigateToReputation -> navigate(Route.Reputation.route)
        is NavigationAction.NavigateToHostedRooms -> navigate(Route.HostedRooms.route)
        is NavigationAction.NavigateToRoomReport -> navigate("roomReport/${action.roomId}")
        is NavigationAction.NavigateToSessionControl -> navigate("sessionControl/${action.roomId}/${action.pin}")
        is NavigationAction.NavigateToCoinHistory -> navigate(Route.CoinHistory.route)
        is NavigationAction.NavigateToEarnings -> navigate(Route.Earnings.route)
        is NavigationAction.NavigateToSettings -> navigate(Route.Settings.route)
        is NavigationAction.NavigateBack -> popBackStack()
    }
}

@Composable
actual fun AppNavHost() {
    val navController = rememberNavController()
    val shellViewModel: AppShellViewModel = koinScreenViewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = AppTab.fromRoute(backStackEntry?.destination?.route)
    val activity = LocalContext.current.findComponentActivity()

    DisposableEffect(activity, navController) {
        val listener = Consumer<Intent> { intent -> navController.handleDeepLink(intent) }

        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }
    LaunchedEffect(shellViewModel) {
        shellViewModel.event.collect { event ->
            when (event) {
                is AppShellEvent.NavigateToTab -> navController.handleNavigationAction(
                    NavigationAction.NavigateToTab(event.tab)
                )
                is AppShellEvent.RequireSignIn -> navController.handleNavigationAction(
                    NavigationAction.NavigateToSignIn
                )
            }
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // 탭 루트 4개에서만 하단 바 표시 (스펙 §1-2)
            if (currentTab != null) {
                PassmateBottomTabBar(
                    selectedTab = currentTab,
                    onSelectTab = { shellViewModel.onAction(AppShellAction.SelectTab(it)) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.Home.route) {
                // 홈 탭 = 입장 폼 인라인 (M-01 v6) — JoinScreen 재사용
                JoinScreen(onNavigate = navController::handleNavigationAction)
            }
            composable(Route.RoomList.route) {
                RoomListScreen(onNavigate = navController::handleNavigationAction)
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
            composable(Route.Result.route) { backStackEntry ->
                ResultScreen(
                    roomId = backStackEntry.arguments?.getString("roomId")?.toLongOrNull() ?: -1L,
                    onNavigate = navController::handleNavigationAction
                )
            }
            composable(Route.Payment.route) { backStackEntry ->
                PaymentScreen(
                    pin = backStackEntry.arguments?.getString("pin").orEmpty(),
                    onNavigate = navController::handleNavigationAction
                )
            }
            composable(Route.MyInfo.route) {
                MyInfoScreen(onNavigate = navController::handleNavigationAction)
            }
            composable(Route.JoinedRooms.route) {
                JoinedRoomsScreen(onNavigate = navController::handleNavigationAction)
            }
            composable(Route.Reputation.route) {
                ReputationScreen(onNavigate = navController::handleNavigationAction)
            }
            composable(Route.HostedRooms.route) {
                HostedRoomsScreen(onNavigate = navController::handleNavigationAction)
            }
            composable(Route.RoomReport.route) { backStackEntry ->
                RoomReportScreen(
                    roomId = backStackEntry.arguments?.getString("roomId")?.toLongOrNull() ?: -1L,
                    onNavigate = navController::handleNavigationAction
                )
            }
            composable(Route.SessionControl.route) { backStackEntry ->
                SessionControlScreen(
                    roomId = backStackEntry.arguments?.getString("roomId")?.toLongOrNull() ?: -1L,
                    pin = backStackEntry.arguments?.getString("pin").orEmpty(),
                    onNavigate = navController::handleNavigationAction
                )
            }
            composable(Route.CoinHistory.route) {
                CoinHistoryScreen(onNavigate = navController::handleNavigationAction)
            }
            composable(Route.Earnings.route) {
                EarningsScreen(onNavigate = navController::handleNavigationAction)
            }
            composable(Route.Settings.route) {
                SettingsScreen(onNavigate = navController::handleNavigationAction)
            }
        }
    }
}
