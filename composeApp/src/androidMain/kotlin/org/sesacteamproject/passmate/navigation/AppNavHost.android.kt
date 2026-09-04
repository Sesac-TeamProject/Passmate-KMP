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
import org.sesacteamproject.passmate.ui.mypage.DeleteAccountScreen
import org.sesacteamproject.passmate.ui.mypage.JoinedRoomsScreen
import org.sesacteamproject.passmate.ui.mypage.MyInfoScreen
import org.sesacteamproject.passmate.ui.mypage.ReputationScreen
import org.sesacteamproject.passmate.ui.payment.CoinChargeScreen
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

// SignIn 엔트리 제거 — popBackStack(route)는 optional 인자가 붙은 SignIn destination과 id 해시가 달라
// 조용히 실패할 수 있다(Navigation 2.7.7). OAuth 딥링크가 SignIn 엔트리를 하나 더 만들 수도 있어
// currentDestination 기준으로 걷어낸다 (스펙 §4-1)
private fun NavHostController.popSignInEntries() {
    while (currentDestination?.route?.startsWith(Route.SignIn.route) == true) {
        if (!popBackStack()) {
            break
        }
    }
}

// 복귀 중복 판정용 라우트 템플릿 (스펙 §4-0). handleNavigationAction의 navigate 대상과 1:1로 유지한다.
// 인자는 비교하지 않는다 — 복귀 대상의 인자는 가드가 걸린 화면의 것과 항상 같다.
// else를 두지 않는다 — 새 NavigationAction이 생기면 여기서 컴파일이 깨져 템플릿 누락을 알려준다
private fun NavigationAction.destinationTemplate(): String? {
    return when (this) {
        is NavigationAction.NavigateToHome -> Route.Home.route
        is NavigationAction.NavigateToTab -> tab.route
        is NavigationAction.NavigateToRoomList -> Route.RoomList.route
        // pin 없는 Join은 handleNavigationAction이 홈으로 보낸다 (스펙 §1-1)
        is NavigationAction.NavigateToJoin -> if (pin != null) Route.Join.route else Route.Home.route
        is NavigationAction.NavigateToPayment -> Route.Payment.route
        is NavigationAction.NavigateToCoinHistory -> Route.CoinHistory.route
        is NavigationAction.NavigateToCoinCharge -> Route.CoinCharge.route
        is NavigationAction.NavigateToWaiting -> Route.Waiting.route
        is NavigationAction.NavigateToPlay -> Route.Play.route
        is NavigationAction.NavigateToResult -> Route.Result.route
        is NavigationAction.NavigateToMyInfo -> Route.MyInfo.route
        is NavigationAction.NavigateToReputation -> Route.Reputation.route
        is NavigationAction.NavigateToHostedRooms -> Route.HostedRooms.route
        is NavigationAction.NavigateToRoomReport -> Route.RoomReport.route
        is NavigationAction.NavigateToSessionControl -> Route.SessionControl.route
        is NavigationAction.NavigateToEarnings -> Route.Earnings.route
        is NavigationAction.NavigateToDeleteAccount -> Route.DeleteAccount.route
        // 복귀 대상이 될 수 없는 액션 — SignIn·로그인 성공 처리·뒤로가기 (스펙 §0)
        is NavigationAction.NavigateToSignIn -> null
        is NavigationAction.NavigateAfterSignIn -> null
        is NavigationAction.NavigateBack -> null
    }
}

private fun NavHostController.handleNavigationAction(action: NavigationAction) {
    when (action) {
        is NavigationAction.NavigateToHome -> navigateHome()
        is NavigationAction.NavigateToTab -> navigateToTab(action.tab)
        is NavigationAction.NavigateToRoomList -> navigate(Route.RoomList.route)
        is NavigationAction.NavigateToSignIn -> navigate(Route.SignIn.route)
        // AppNavHost의 onNavigate 래퍼가 가로채므로 여기로는 오지 않는다. when 완전성용 방어 분기
        is NavigationAction.NavigateAfterSignIn -> navigateHome()
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
        is NavigationAction.NavigateToCoinCharge -> navigate(Route.CoinCharge.route)
        is NavigationAction.NavigateToEarnings -> navigate(Route.Earnings.route)
        is NavigationAction.NavigateToDeleteAccount -> navigate(Route.DeleteAccount.route)
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

    // SignIn 관련 두 액션만 셸로 보내고 나머지 17개 분기는 기존 확장함수가 처리한다 (스펙 §4-1)
    val onNavigate: (NavigationAction) -> Unit = { action ->
        when (action) {
            is NavigationAction.NavigateToSignIn -> {
                shellViewModel.onAction(AppShellAction.RememberPendingRoute(action.pendingRoute))
                navController.navigate(Route.SignIn.route)
            }
            is NavigationAction.NavigateAfterSignIn ->
                shellViewModel.onAction(AppShellAction.ResumeAfterSignIn)
            else -> navController.handleNavigationAction(action)
        }
    }

    DisposableEffect(activity, navController) {
        val listener = Consumer<Intent> { intent -> navController.handleDeepLink(intent) }

        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }
    LaunchedEffect(shellViewModel) {
        shellViewModel.event.collect { event ->
            when (event) {
                is AppShellEvent.NavigateToTab -> navController.navigateToTab(event.tab)
                is AppShellEvent.RequireSignIn -> navController.navigate(Route.SignIn.route)
                is AppShellEvent.ResumePendingRoute -> {
                    navController.popSignInEntries()

                    // 탭 복귀는 중복 판정에서 제외한다 — navigateToTab이 popUpTo(Home)+launchSingleTop이라
                    // 중복이 생기지 않고(스펙 §4-0), 건너뛰면 탭 화면이 재생성되지 않아 로그인 이전 상태가
                    // 그대로 남는다. iOS는 sessionGeneration 증가, Desktop은 최상단만 컴포즈라 둘 다 재생성한다
                    val isTabResume = event.pendingRoute is NavigationAction.NavigateToTab
                    // 복귀 대상이 이미 최상단이면 이동하지 않는다 — 같은 화면이 두 번 쌓이는 것을 막는다 (스펙 §4-0)
                    val isAlreadyOnTop =
                        navController.currentDestination?.route == event.pendingRoute.destinationTemplate()

                    if (isTabResume || !isAlreadyOnTop) {
                        onNavigate(event.pendingRoute)
                    }
                }
                is AppShellEvent.NavigateToHome -> navController.navigateHome()
            }
        }
    }
    Scaffold(
        // 0을 유지한다 — 화면 배경이 상태바 뒤까지 깔려야 iOS와 같아진다.
        // 상단 인셋은 각 화면이 배경 뒤에 statusBarsPadding으로 직접 준다
        // (iOS도 화면마다 `.background(색.ignoresSafeArea())`로 같은 일을 한다)
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
                JoinScreen(onNavigate = onNavigate)
            }
            composable(Route.RoomList.route) {
                RoomListScreen(onNavigate = onNavigate)
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
                    onNavigate = onNavigate
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
                    onNavigate = onNavigate
                )
            }
            composable(Route.Waiting.route) { backStackEntry ->
                WaitingScreen(
                    pin = backStackEntry.arguments?.getString("pin").orEmpty(),
                    onNavigate = onNavigate
                )
            }
            composable(Route.Play.route) { backStackEntry ->
                PlayScreen(
                    pin = backStackEntry.arguments?.getString("pin").orEmpty(),
                    onNavigate = onNavigate
                )
            }
            composable(Route.Result.route) { backStackEntry ->
                ResultScreen(
                    roomId = backStackEntry.arguments?.getString("roomId")?.toLongOrNull() ?: -1L,
                    onNavigate = onNavigate
                )
            }
            composable(Route.Payment.route) { backStackEntry ->
                PaymentScreen(
                    pin = backStackEntry.arguments?.getString("pin").orEmpty(),
                    onNavigate = onNavigate
                )
            }
            composable(Route.MyInfo.route) {
                MyInfoScreen(onNavigate = onNavigate)
            }
            composable(Route.JoinedRooms.route) {
                JoinedRoomsScreen(onNavigate = onNavigate)
            }
            composable(Route.Reputation.route) {
                ReputationScreen(onNavigate = onNavigate)
            }
            composable(Route.HostedRooms.route) {
                HostedRoomsScreen(onNavigate = onNavigate)
            }
            composable(Route.RoomReport.route) { backStackEntry ->
                RoomReportScreen(
                    roomId = backStackEntry.arguments?.getString("roomId")?.toLongOrNull() ?: -1L,
                    onNavigate = onNavigate
                )
            }
            composable(Route.SessionControl.route) { backStackEntry ->
                SessionControlScreen(
                    roomId = backStackEntry.arguments?.getString("roomId")?.toLongOrNull() ?: -1L,
                    pin = backStackEntry.arguments?.getString("pin").orEmpty(),
                    onNavigate = onNavigate
                )
            }
            composable(Route.CoinHistory.route) {
                CoinHistoryScreen(onNavigate = onNavigate)
            }
            composable(Route.CoinCharge.route) {
                CoinChargeScreen(onNavigate = onNavigate)
            }
            composable(Route.Earnings.route) {
                EarningsScreen(onNavigate = onNavigate)
            }
            composable(Route.DeleteAccount.route) {
                DeleteAccountScreen(onNavigate = onNavigate)
            }
        }
    }
}
