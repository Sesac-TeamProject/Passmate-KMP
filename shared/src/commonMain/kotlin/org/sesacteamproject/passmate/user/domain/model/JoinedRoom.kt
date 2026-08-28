package org.sesacteamproject.passmate.user.domain.model

// 참여한(종료된) 방 1건 — 리포트 진입 대상 (M-08 리스트, FR-032). dateLabel은 서버 포맷 문자열
data class JoinedRoom(
    val roomId: Long,
    val title: String,
    val dateLabel: String,
    val questionCount: Int,
    val myScore: Double?,
    val myRank: Int?,
    val hasReport: Boolean
)
