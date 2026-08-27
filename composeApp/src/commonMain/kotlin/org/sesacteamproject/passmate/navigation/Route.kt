package org.sesacteamproject.passmate.navigation

// 공통 라우트 규격 (규칙 §2-1-1) — 이름·인자는 3플랫폼 동일 유지. iosApp Route.swift와 1:1
sealed class Route(val route: String) {

    data object Home : Route("home")

    data object SignIn : Route("signIn")

    data object Join : Route("join?pin={pin}")

    data object Waiting : Route("waiting/{pin}")

    data object Play : Route("play/{pin}")

    data object Result : Route("result/{participationId}")

    data object MyInfo : Route("myInfo")

    data object Payment : Route("payment/{pin}")

    data object Settings : Route("settings")
}
