package org.sesacteamproject.passmate.navigation

// 공통 라우트 규격 (규칙 §2-1-1) — 이름·인자는 3플랫폼 동일 유지. iosApp Route.swift와 1:1
sealed class Route(val route: String) {

    data object Home : Route("home")

    // 공개 방 목록·탐색 (M-11) — 홈에서 진입하는 방 찾기 화면
    data object RoomList : Route("roomList")

    data object SignIn : Route("signIn")

    data object Join : Route("join?pin={pin}")

    data object Waiting : Route("waiting/{pin}")

    data object Play : Route("play/{pin}")

    // 결과·리포트 화면 — 백엔드 /rooms/{roomId}/results/me 기준으로 roomId를 인자로 받는다
    // (2026-08-28 백엔드 명세서 정합. 마이페이지(파트2 US6)도 roomId로 진입한다)
    data object Result : Route("result/{roomId}")

    data object MyInfo : Route("myInfo")

    // 내 명성·뱃지 상세 (M-09) — 마이페이지에서 진입
    data object Reputation : Route("reputation")

    // 내가 만든 방 (M-13) — 마이페이지에서 진입, 새 방 만들기 시트 포함
    data object HostedRooms : Route("hostedRooms")

    // 방 리포트 (M-14) — 내가 만든 방 › 종료 › 상세
    data object RoomReport : Route("roomReport/{roomId}")

    // 진행 리모컨 (M-T2) — 내가 만든 방 › 진행 중 › 진행
    data object SessionControl : Route("sessionControl/{roomId}/{pin}")

    data object Payment : Route("payment/{pin}")

    // 코인·결제 내역 (M-12) — 마이페이지에서 진입
    data object CoinHistory : Route("coinHistory")

    data object Settings : Route("settings")
}
