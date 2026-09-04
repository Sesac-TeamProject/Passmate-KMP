package org.sesacteamproject.passmate.navigation

// 하단 4탭 (피그마 v6) — 라우트·라벨·로그인 필수 여부는 3플랫폼 동일 (규칙 §2-1-1)
enum class AppTab(
    val route: String,
    val label: String,
    val requiresSignIn: Boolean
) {
    HOME(Route.Home.route, "홈", false),
    HOSTED_ROOMS(Route.HostedRooms.route, "내가 만든 방", true),
    JOINED_ROOMS(Route.JoinedRooms.route, "참여한 방", true),
    MY_INFO(Route.MyInfo.route, "마이", true);

    companion object {
        // 탭 루트에서 push되지만 시안이 하단 탭바를 유지하는 화면들 (M-12-x 전부 · M-14 방 리포트).
        // 세션 플로우(대기실·풀이·결과)와 M-09 명성·M-T4 정산은 시안에 탭바가 없어 넣지 않는다
        private val DETAIL_ROUTE_OWNERS: Map<String, AppTab> = mapOf(
            Route.EditProfile.route to MY_INFO,
            Route.CharacterEdit.route to MY_INFO,
            Route.SettlementAccount.route to MY_INFO,
            Route.CoinCharge.route to MY_INFO,
            Route.PaymentMethod.route to MY_INFO,
            Route.CoinHistory.route to MY_INFO,
            Route.NotificationSettings.route to MY_INFO,
            Route.DeleteAccount.route to MY_INFO,
            Route.RoomReport.route to HOSTED_ROOMS
        )

        fun fromRoute(route: String?): AppTab? {
            return entries.firstOrNull { it.route == route }
        }

        // 하단 탭바를 표시할지와 어떤 탭을 켤지를 함께 정한다. null이면 탭바를 숨긴다
        fun barOwnerOf(route: String?): AppTab? {
            return fromRoute(route) ?: DETAIL_ROUTE_OWNERS[route]
        }
    }
}
