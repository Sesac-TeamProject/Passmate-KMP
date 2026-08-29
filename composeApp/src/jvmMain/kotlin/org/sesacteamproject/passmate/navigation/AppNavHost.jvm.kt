package org.sesacteamproject.passmate.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import org.sesacteamproject.passmate.ui.auth.SignInScreen
import org.sesacteamproject.passmate.ui.home.HomeScreen
import org.sesacteamproject.passmate.ui.home.RoomListScreen
import org.sesacteamproject.passmate.ui.join.JoinScreen
import org.sesacteamproject.passmate.ui.hostroom.HostedRoomsScreen
import org.sesacteamproject.passmate.ui.hostroom.RoomReportScreen
import org.sesacteamproject.passmate.ui.hostroom.SessionControlScreen
import org.sesacteamproject.passmate.ui.mypage.MyInfoScreen
import org.sesacteamproject.passmate.ui.mypage.ReputationScreen
import org.sesacteamproject.passmate.ui.payment.CoinHistoryScreen
import org.sesacteamproject.passmate.ui.payment.PaymentScreen
import org.sesacteamproject.passmate.ui.play.PlayScreen
import org.sesacteamproject.passmate.ui.result.ResultScreen
import org.sesacteamproject.passmate.ui.waiting.WaitingScreen

// Desktop은 상태 기반 라우트 상태머신 (규칙 §2-1). 라우트 인자는 목적지 엔트리로 보관한다
private sealed interface JvmDestination {

    data object Home : JvmDestination

    data object RoomList : JvmDestination

    data object SignIn : JvmDestination

    data class Join(val pin: String?) : JvmDestination

    data class Payment(val pin: String) : JvmDestination

    data object CoinHistory : JvmDestination

    data class Waiting(val pin: String) : JvmDestination

    data class Play(val pin: String) : JvmDestination

    data class Result(val roomId: Long) : JvmDestination

    data object MyInfo : JvmDestination

    data object Reputation : JvmDestination

    data object HostedRooms : JvmDestination

    data class RoomReport(val roomId: Long) : JvmDestination

    data class SessionControl(val roomId: Long, val pin: String) : JvmDestination
}

@Composable
actual fun AppNavHost() {
    val routeStack = remember { mutableStateListOf<JvmDestination>(JvmDestination.Home) }
    val currentDestination = routeStack.last()
    val onNavigate: (NavigationAction) -> Unit = { action ->
        when (action) {
            is NavigationAction.NavigateToHome -> {
                routeStack.clear()
                routeStack.add(JvmDestination.Home)
            }
            is NavigationAction.NavigateToRoomList -> routeStack.add(JvmDestination.RoomList)
            is NavigationAction.NavigateToSignIn -> routeStack.add(JvmDestination.SignIn)
            is NavigationAction.NavigateToJoin -> routeStack.add(JvmDestination.Join(action.pin))
            is NavigationAction.NavigateToPayment -> routeStack.add(JvmDestination.Payment(action.pin))
            is NavigationAction.NavigateToWaiting -> routeStack.add(JvmDestination.Waiting(action.pin))
            is NavigationAction.NavigateToPlay -> routeStack.add(JvmDestination.Play(action.pin))
            is NavigationAction.NavigateToResult -> {
                // 세션 플로우 백스택 클리어 후 결과 진입 (규칙 §2-1-2)
                routeStack.removeAll { it is JvmDestination.Waiting || it is JvmDestination.Play }
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
            is NavigationAction.NavigateBack -> {
                if (routeStack.size > 1) {
                    routeStack.removeAt(routeStack.lastIndex)
                }
            }
        }
    }

    when (currentDestination) {
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
        else -> HomeScreen(onNavigate = onNavigate)
    }
}
