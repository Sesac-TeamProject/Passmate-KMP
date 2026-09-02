package org.sesacteamproject.passmate.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.sesacteamproject.passmate.component.PassmateBottomTabBar
import org.sesacteamproject.passmate.di.koinScreenViewModel
import org.sesacteamproject.passmate.ui.auth.SignInScreen
import org.sesacteamproject.passmate.ui.home.RoomListScreen
import org.sesacteamproject.passmate.ui.hostroom.HostedRoomsScreen
import org.sesacteamproject.passmate.ui.hostroom.RoomReportScreen
import org.sesacteamproject.passmate.ui.hostroom.SessionControlScreen
import org.sesacteamproject.passmate.ui.join.JoinScreen
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

// Desktop은 상태 기반 라우트 상태머신 (규칙 §2-1). 탭 전환은 스택을 [탭 루트]로 교체한다 (스펙 §1-4)
private sealed interface JvmDestination {
    data object Home : JvmDestination
    data object RoomList : JvmDestination
    data object SignIn : JvmDestination
    data class Join(val pin: String?) : JvmDestination
    data class Payment(val pin: String) : JvmDestination
    data object CoinHistory : JvmDestination
    data object CoinCharge : JvmDestination
    data class Waiting(val pin: String) : JvmDestination
    data class Play(val pin: String) : JvmDestination
    data class Result(val roomId: Long) : JvmDestination
    data object MyInfo : JvmDestination
    data object JoinedRooms : JvmDestination
    data object Reputation : JvmDestination
    data object HostedRooms : JvmDestination
    data class RoomReport(val roomId: Long) : JvmDestination
    data class SessionControl(val roomId: Long, val pin: String) : JvmDestination
    data object Earnings : JvmDestination
    data object DeleteAccount : JvmDestination
}

private fun JvmDestination.toTab(): AppTab? {
    return when (this) {
        is JvmDestination.Home -> AppTab.HOME
        is JvmDestination.HostedRooms -> AppTab.HOSTED_ROOMS
        is JvmDestination.JoinedRooms -> AppTab.JOINED_ROOMS
        is JvmDestination.MyInfo -> AppTab.MY_INFO
        else -> null
    }
}

private fun AppTab.toDestination(): JvmDestination {
    return when (this) {
        AppTab.HOME -> JvmDestination.Home
        AppTab.HOSTED_ROOMS -> JvmDestination.HostedRooms
        AppTab.JOINED_ROOMS -> JvmDestination.JoinedRooms
        AppTab.MY_INFO -> JvmDestination.MyInfo
    }
}

private fun JvmDestination.isSessionFlow(): Boolean {
    return this is JvmDestination.Join ||
        this is JvmDestination.Payment ||
        this is JvmDestination.Waiting ||
        this is JvmDestination.Play
}

@Composable
actual fun AppNavHost() {
    val routeStack = remember { mutableStateListOf<JvmDestination>(JvmDestination.Home) }
    val shellViewModel: AppShellViewModel = koinScreenViewModel()
    val currentDestination = routeStack.last()
    val currentTab = currentDestination.toTab()
    val switchTab: (AppTab) -> Unit = { tab ->
        routeStack.clear()
        routeStack.add(tab.toDestination())
    }
    val onNavigate: (NavigationAction) -> Unit = { action ->
        when (action) {
            is NavigationAction.NavigateToHome -> switchTab(AppTab.HOME)
            is NavigationAction.NavigateToTab -> switchTab(action.tab)
            is NavigationAction.NavigateToRoomList -> routeStack.add(JvmDestination.RoomList)
            is NavigationAction.NavigateToSignIn -> {
                shellViewModel.onAction(AppShellAction.RememberPendingRoute(action.pendingRoute))
                routeStack.add(JvmDestination.SignIn)
            }
            is NavigationAction.NavigateAfterSignIn ->
                shellViewModel.onAction(AppShellAction.ResumeAfterSignIn)
            is NavigationAction.NavigateToJoin -> {
                // 홈 탭이 곧 입장 폼 — pin 없는 Join은 홈 탭으로 (스펙 §1-1)
                if (action.pin != null) {
                    routeStack.add(JvmDestination.Join(action.pin))
                } else {
                    switchTab(AppTab.HOME)
                }
            }
            is NavigationAction.NavigateToPayment -> routeStack.add(JvmDestination.Payment(action.pin))
            is NavigationAction.NavigateToWaiting -> routeStack.add(JvmDestination.Waiting(action.pin))
            is NavigationAction.NavigateToPlay -> routeStack.add(JvmDestination.Play(action.pin))
            is NavigationAction.NavigateToResult -> {
                // 세션 플로우 엔트리(Join·Payment·Waiting·Play)만 제거, 탭 루트 유지 (규칙 §2-1-2, 스펙 §1-5)
                routeStack.removeAll { it.isSessionFlow() }
                routeStack.add(JvmDestination.Result(action.roomId))
            }
            is NavigationAction.NavigateToMyInfo -> routeStack.add(JvmDestination.MyInfo)
            is NavigationAction.NavigateToReputation -> routeStack.add(JvmDestination.Reputation)
            is NavigationAction.NavigateToHostedRooms -> routeStack.add(JvmDestination.HostedRooms)
            is NavigationAction.NavigateToRoomReport -> routeStack.add(JvmDestination.RoomReport(action.roomId))
            is NavigationAction.NavigateToSessionControl -> routeStack.add(
                JvmDestination.SessionControl(action.roomId, action.pin)
            )
            is NavigationAction.NavigateToCoinHistory -> routeStack.add(JvmDestination.CoinHistory)
            is NavigationAction.NavigateToCoinCharge -> routeStack.add(JvmDestination.CoinCharge)
            is NavigationAction.NavigateToEarnings -> routeStack.add(JvmDestination.Earnings)
            is NavigationAction.NavigateToDeleteAccount -> routeStack.add(JvmDestination.DeleteAccount)
            is NavigationAction.NavigateBack -> {
                if (routeStack.size > 1) {
                    routeStack.removeAt(routeStack.lastIndex)
                }
            }
        }
    }

    LaunchedEffect(shellViewModel) {
        shellViewModel.event.collect { event ->
            when (event) {
                is AppShellEvent.NavigateToTab -> onNavigate(NavigationAction.NavigateToTab(event.tab))
                is AppShellEvent.RequireSignIn -> routeStack.add(JvmDestination.SignIn)
                is AppShellEvent.ResumePendingRoute -> {
                    routeStack.removeAll { it is JvmDestination.SignIn }
                    onNavigate(event.pendingRoute)
                    // 같은 화면이 중복 push됐으면 걷어낸다 — JvmDestination은 data class/object라 구조적 동등 비교가 된다 (스펙 §4-0)
                    if (routeStack.size >= 2 && routeStack.last() == routeStack[routeStack.lastIndex - 1]) {
                        routeStack.removeAt(routeStack.lastIndex)
                    }
                }
                is AppShellEvent.NavigateToHome -> onNavigate(NavigationAction.NavigateToHome)
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (currentDestination) {
                is JvmDestination.Home -> JoinScreen(onNavigate = onNavigate)
                is JvmDestination.RoomList -> RoomListScreen(onNavigate = onNavigate)
                is JvmDestination.SignIn -> SignInScreen(onNavigate = onNavigate)
                is JvmDestination.Join -> JoinScreen(
                    initialPin = currentDestination.pin,
                    onNavigate = onNavigate
                )
                is JvmDestination.Payment -> PaymentScreen(
                    pin = currentDestination.pin,
                    onNavigate = onNavigate
                )
                is JvmDestination.CoinHistory -> CoinHistoryScreen(onNavigate = onNavigate)
                is JvmDestination.CoinCharge -> CoinChargeScreen(onNavigate = onNavigate)
                is JvmDestination.Earnings -> EarningsScreen(onNavigate = onNavigate)
                is JvmDestination.DeleteAccount -> DeleteAccountScreen(onNavigate = onNavigate)
                is JvmDestination.Waiting -> WaitingScreen(
                    pin = currentDestination.pin,
                    onNavigate = onNavigate
                )
                is JvmDestination.Play -> PlayScreen(
                    pin = currentDestination.pin,
                    onNavigate = onNavigate
                )
                is JvmDestination.Result -> ResultScreen(
                    roomId = currentDestination.roomId,
                    onNavigate = onNavigate
                )
                is JvmDestination.MyInfo -> MyInfoScreen(onNavigate = onNavigate)
                is JvmDestination.JoinedRooms -> JoinedRoomsScreen(onNavigate = onNavigate)
                is JvmDestination.Reputation -> ReputationScreen(onNavigate = onNavigate)
                is JvmDestination.HostedRooms -> HostedRoomsScreen(onNavigate = onNavigate)
                is JvmDestination.RoomReport -> RoomReportScreen(
                    roomId = currentDestination.roomId,
                    onNavigate = onNavigate
                )
                is JvmDestination.SessionControl -> SessionControlScreen(
                    roomId = currentDestination.roomId,
                    pin = currentDestination.pin,
                    onNavigate = onNavigate
                )
            }
        }
        // 탭 루트 4개에서만 하단 바 표시 (스펙 §1-2)
        if (currentTab != null) {
            PassmateBottomTabBar(
                selectedTab = currentTab,
                onSelectTab = { shellViewModel.onAction(AppShellAction.SelectTab(it)) }
            )
        }
    }
}
