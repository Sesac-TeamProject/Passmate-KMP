package org.sesacteamproject.passmate.navigation

// 화면은 이 액션만 방출하고, 실제 이동은 플랫폼 셸(AppNavHost)이 수행한다
sealed interface NavigationAction {

    data object NavigateToHome : NavigationAction

    // 하단 탭 전환 — 셸(AppShellViewModel) 가드 통과 후에만 발행된다
    data class NavigateToTab(val tab: AppTab) : NavigationAction

    data object NavigateToRoomList : NavigationAction

    // 로그인 유도 — pendingRoute는 로그인 성공 후 복귀할 목적지. null이면 홈으로 (규칙 §7, 스펙 §2-1)
    data class NavigateToSignIn(val pendingRoute: NavigationAction? = null) : NavigationAction

    // 로그인 성공 — 목적지 결정은 셸(AppShellViewModel)에 위임한다 (스펙 §2-4)
    data object NavigateAfterSignIn : NavigationAction

    data class NavigateToJoin(val pin: String? = null) : NavigationAction

    // 유료 방 결제 입장 (M-01 v2) — pin으로 방 정보·코인을 로드한다
    data class NavigateToPayment(val pin: String) : NavigationAction

    data object NavigateToCoinHistory : NavigationAction

    // 코인 충전 (M-12-4·M-12-6) — 마이 탭 보유 코인 행에서 진입
    data object NavigateToCoinCharge : NavigationAction

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

    // 회원 탈퇴 (M-12-12) — 설정에서 진입
    data object NavigateToDeleteAccount : NavigationAction

    // 계정 정보 변경 (M-12-1) — 마이 › 계정 정보 행에서 진입
    data object NavigateToEditProfile : NavigationAction

    // 내 캐릭터 변경 (M-12-7) — M-12-1의 "캐릭터 바꾸기 →"에서 진입
    data object NavigateToCharacterEdit : NavigationAction

    // 정산 계좌 등록 (M-12-3) — 마이 · 정산(M-T4) 양쪽에서 진입
    data object NavigateToSettlementAccount : NavigationAction

    // 결제 수단 관리 (M-12-8)
    data object NavigateToPaymentMethod : NavigationAction

    // 알림 설정 (M-12-10)
    data object NavigateToNotificationSettings : NavigationAction

    data object NavigateBack : NavigationAction
}
