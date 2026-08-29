package org.sesacteamproject.passmate.navigation

// 화면은 이 액션만 방출하고, 실제 이동은 플랫폼 셸(AppNavHost)이 수행한다
sealed interface NavigationAction {

    data object NavigateToHome : NavigationAction

    data object NavigateToRoomList : NavigationAction

    data object NavigateToSignIn : NavigationAction

    data class NavigateToJoin(val pin: String? = null) : NavigationAction

    // 유료 방 결제 입장 (M-01 v2) — pin으로 방 정보·코인을 로드한다
    data class NavigateToPayment(val pin: String) : NavigationAction

    data object NavigateToCoinHistory : NavigationAction

    data class NavigateToWaiting(val pin: String) : NavigationAction

    data class NavigateToPlay(val pin: String) : NavigationAction

    data class NavigateToResult(val roomId: Long) : NavigationAction

    data object NavigateToMyInfo : NavigationAction

    // 내 명성·뱃지 상세 (M-09) — 마이페이지에서 진입
    data object NavigateToReputation : NavigationAction

    // 내가 만든 방 (M-13) — 마이페이지에서 진입
    data object NavigateToHostedRooms : NavigationAction

    data object NavigateBack : NavigationAction
}
