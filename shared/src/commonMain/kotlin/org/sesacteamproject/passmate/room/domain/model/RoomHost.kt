package org.sesacteamproject.passmate.room.domain.model

data class RoomHost(
    // 호스트 userId — 선생님 프로필 시트(M-10) 진입용 (contracts 2026-08-29 추가)
    val userId: Long?,
    val nickname: String,
    val level: Int?,
    val avgStars: Double?,
    val ratingCount: Int?
)
