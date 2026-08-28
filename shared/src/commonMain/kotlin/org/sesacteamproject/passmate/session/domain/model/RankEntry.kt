package org.sesacteamproject.passmate.session.domain.model

data class RankEntry(
    val rank: Int,
    val participantId: Long,
    val nickname: String,
    val avatarId: Int?,
    val total: Double
)
