package org.sesacteamproject.passmate.user.domain.model

import org.sesacteamproject.passmate.room.domain.model.HostLevel

// 내 명성 (GET /users/me/grade) — 명성 상세(M-09)·내가 만든 방 상단(M-13) 렌더링용 (FR-045~048)
data class MyGrade(
    val level: HostLevel,
    val achievedAt: String?,
    val stats: GradeStats,
    val next: NextGrade?
)

// 집계값 — 참여·운영 실적 (학생도 실적으로 명성을 쌓아 선생님이 된다)
data class GradeStats(
    val participationCount: Int,
    val avgAccuracyPercent: Int?,
    val roomCount: Int,
    val totalStudents: Int,
    val avgStars: Double?,
    val ratingCount: Int
)

// 다음 승급 정보 — null이면 최고 등급(Lv.5)
data class NextGrade(
    val level: HostLevel,
    val progressPercent: Int,
    val criteria: List<GradeCriterion>
)

// 승급 조건 1건 — met면 "✓ {current}", 아니면 "{current} / {target}"로 렌더링 (M-09)
data class GradeCriterion(
    val label: String,
    val current: Double,
    val target: Double,
    val met: Boolean
)
