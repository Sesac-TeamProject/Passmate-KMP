package org.sesacteamproject.passmate.user.domain.model

// 업적 뱃지 8종 (contracts §BadgeType, 디자인 시스템 §뱃지) — M-09 내 뱃지·M-10 프로필 시트
enum class BadgeType(val wireValue: String, val label: String) {
    FIRST_ROOM("FIRST_ROOM", "첫 방 개설"),
    ROOMS_10("ROOMS_10", "방 10회 운영"),
    STUDENTS_100("STUDENTS_100", "학생 100명"),
    RATING_45("RATING_45", "평가 4.5+"),
    RATINGS_50("RATINGS_50", "평가 50개 받기"),
    STREAK_30("STREAK_30", "30일 연속 활동"),
    FIRST_PAID_ROOM("FIRST_PAID_ROOM", "유료 방 첫 개설"),
    AI_SETS_50("AI_SETS_50", "AI 세트 50개");

    companion object {

        fun from(wireValue: String?): BadgeType? {
            return entries.firstOrNull { it.wireValue == wireValue }
        }
    }
}

// 뱃지 1건 — 미획득이면 진행도(progressCurrent/Target)로 "12/30" 표시 (GET /users/me/badges)
// progressTarget이 Double인 이유: RATING_45("평가 4.5+")처럼 소수 목표가 있다. 정수로 좁히면 4.5가 4가 된다
data class Badge(
    val type: BadgeType,
    val earned: Boolean,
    val earnedAt: String?,
    val progressCurrent: Int?,
    val progressTarget: Double?
)
