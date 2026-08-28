package org.sesacteamproject.passmate.session.domain.model

import org.sesacteamproject.passmate.room.domain.model.RoomStatus

// GET /rooms/{roomId}/session 재접속 상태 복구 스냅샷 (contracts §재접속 프로토콜)
data class SessionSnapshot(
    val status: RoomStatus,
    val ts: String,
    val questionCount: Int?,
    val currentQuestion: SessionQuestion?,
    val myAnswers: List<SubmittedAnswer>,
    val totalScore: Double?,
    val rank: Int?,
    val ranking: List<RankEntry>,
    val isLocked: Boolean
)
