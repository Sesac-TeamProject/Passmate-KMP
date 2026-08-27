package org.sesacteamproject.passmate.core.storage

expect class TokenStorage {

    // 게스트 토큰은 세션 스코프(메모리 보관) — join 응답으로 받고 앱 종료 시 소멸
    var guestToken: String?

    fun saveMemberTokens(accessToken: String, refreshToken: String)

    fun accessToken(): String?

    fun refreshToken(): String?

    fun clearMemberTokens()
}
