package org.sesacteamproject.passmate.core.storage

expect class TokenStorage {

    // 게스트 토큰은 방 하나에 묶이고 서버가 1시간 뒤 만료시킨다.
    // 앱을 껐다 켜도 남아야 재입장으로 원래 자리에 돌아갈 수 있다 (규칙 §8)
    val guestToken: String?

    // 토큰이 어느 방 것인지 — 다른 방에 들어가면 덮어쓴다
    val guestRoomId: Long?

    fun saveGuestSession(token: String, roomId: Long)

    fun clearGuestSession()

    fun saveMemberTokens(accessToken: String, refreshToken: String)

    fun accessToken(): String?

    fun refreshToken(): String?

    fun clearMemberTokens()
}
