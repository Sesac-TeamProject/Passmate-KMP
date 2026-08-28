package org.sesacteamproject.passmate.room.domain.model

// 내 입장 결과 — 세션 스코프 보관 (게스트 토큰은 TokenStorage가 별도 보관)
data class MyParticipation(
    val participantId: Long,
    val roomId: Long,
    val pin: String,
    val nickname: String,
    val avatarId: Int?,
    val isGuest: Boolean
)
