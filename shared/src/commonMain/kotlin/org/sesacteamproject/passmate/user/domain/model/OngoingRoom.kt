package org.sesacteamproject.passmate.user.domain.model

// 진행 중인 참여 방 — "다시 들어가기"로 재접속한다 (M-08 상단 카드, FR-024)
data class OngoingRoom(
    val roomId: Long,
    val pin: String,
    val title: String,
    val hostNickname: String?,
    val progressLabel: String?
)
