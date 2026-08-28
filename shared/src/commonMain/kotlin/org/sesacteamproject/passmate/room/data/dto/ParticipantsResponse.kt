package org.sesacteamproject.passmate.room.data.dto

import kotlinx.serialization.Serializable

// GET /rooms/{roomId}/participants 응답 — 초기 로딩·재접속 복구용, 이후 변경은 WS 이벤트
@Serializable
data class ParticipantsResponse(
    val participants: List<Entry> = emptyList()
) {

    @Serializable
    data class Entry(
        val participantId: Long,
        val nickname: String,
        val avatarId: Int? = null,
        val isGuest: Boolean = false,
        val isConnected: Boolean = true
    )
}
