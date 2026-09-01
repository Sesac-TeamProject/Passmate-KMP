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

    // 참여한 방 탭 루트 (M-08) — 하단 4탭 셸
    data object JoinedRooms : Route("joinedRooms")

    // 내 명성·뱃지 상세 (M-09) — 마이 탭 프로필 카드에서 진입
    data object Reputation : Route("reputation")

    // 내가 만든 방 (M-13) — 하단 탭 루트, 새 방 만들기 시트 포함
    data object HostedRooms : Route("hostedRooms")

    // 방 리포트 (M-14) — 내가 만든 방 › 종료 › 상세
    data object RoomReport : Route("roomReport/{roomId}")

    // 진행 리모컨 (M-T2) — 내가 만든 방 › 진행 중 › 진행
    data object SessionControl : Route("sessionControl/{roomId}/{pin}")

    // 정산 (M-T4) — 마이 탭에서 진입, 정산 계좌 시트(M-12-3) 포함
    data object Earnings : Route("earnings")

    data object Payment : Route("payment/{pin}")

    // 코인·결제 내역 (M-12) — 마이 탭에서 진입
    data object CoinHistory : Route("coinHistory")

    // 코인 충전 (M-12-4·M-12-6) — 마이 탭 보유 코인 행에서 진입
    data object CoinCharge : Route("coinCharge")

    data object Settings : Route("settings")
}
