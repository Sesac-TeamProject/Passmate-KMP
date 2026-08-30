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
        fun fromRoute(route: String?): AppTab? {
            return entries.firstOrNull { it.route == route }
        }
    }
}
