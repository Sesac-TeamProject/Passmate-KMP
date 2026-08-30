package org.sesacteamproject.passmate.navigation

// 화면은 이 액션만 방출하고, 실제 이동은 플랫폼 셸(AppNavHost)이 수행한다
sealed interface NavigationAction {

    data object NavigateToHome : NavigationAction

    // 하단 탭 전환 — 셸(AppShellViewModel) 가드 통과 후에만 발행된다
    data class NavigateToTab(val tab: AppTab) : NavigationAction

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

    // 내 명성·뱃지 상세 (M-09) — 마이 탭 프로필 카드에서 진입
    data object NavigateToReputation : NavigationAction

    // 내가 만든 방 (M-13) — 하단 탭 루트(탭 바 전용, 직접 push 호출처 없음)
    data object NavigateToHostedRooms : NavigationAction

    // 방 리포트 (M-14) — 내가 만든 방 › 종료 › 상세
    data class NavigateToRoomReport(val roomId: Long) : NavigationAction

    // 진행 리모컨 (M-T2) — 내가 만든 방 › 진행 중 › 진행
    data class NavigateToSessionControl(val roomId: Long, val pin: String) : NavigationAction

    // 정산 (M-T4) — 마이 탭에서 진입
    data object NavigateToEarnings : NavigationAction

    // 설정(내 정보 관리, M-12) — 마이 탭 우상단 "설정"에서 진입 (규칙 §2-1-1: Settings는 MyInfo의 상세 push)
    data object NavigateToSettings : NavigationAction

    data object NavigateBack : NavigationAction
}
