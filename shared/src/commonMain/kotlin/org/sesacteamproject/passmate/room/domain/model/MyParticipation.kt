package org.sesacteamproject.passmate.room.domain.model

// 내 입장 결과 — 세션 스코프 보관 (게스트 토큰은 TokenStorage가 별도 보관)
data class MyParticipation(
    val participantId: Long,
    val roomId: Long,
    val pin: String,
    val nickname: String,
    val avatarId: Int?,
    val isGuest: Boolean,
    // 새 입장이 아니라 원래 참가자 행으로 돌아온 경우 — 화면이 안내 문구를 띄울지 판단한다
    val isRejoined: Boolean = false
)
