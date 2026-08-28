package org.sesacteamproject.passmate.room.domain.model

// 대기실 참가자 1명 (닉네임·회원/게스트·접속 상태 — FR-006)
data class Participant(
    val participantId: Long,
    val nickname: String,
    val avatarId: Int?,
    val isGuest: Boolean,
    val isConnected: Boolean
)
