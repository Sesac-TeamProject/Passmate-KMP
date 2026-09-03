package org.sesacteamproject.passmate.user.data.mapper

import org.sesacteamproject.passmate.payment.data.mapper.toDomain
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.user.data.dto.BadgesResponse
import org.sesacteamproject.passmate.user.data.dto.GradeResponse
import org.sesacteamproject.passmate.user.data.dto.HostProfileResponse
import org.sesacteamproject.passmate.user.data.dto.UserProfileResponse
import kotlin.math.roundToInt
import org.sesacteamproject.passmate.user.data.dto.MyPageResponse
import org.sesacteamproject.passmate.user.data.dto.NotificationSettingsDto
import org.sesacteamproject.passmate.user.domain.model.Badge
import org.sesacteamproject.passmate.user.domain.model.BadgeType
import org.sesacteamproject.passmate.user.domain.model.GradeCriterion
import org.sesacteamproject.passmate.user.domain.model.GradeStats
import org.sesacteamproject.passmate.user.domain.model.HostProfile
import org.sesacteamproject.passmate.user.domain.model.JoinedRoom
import org.sesacteamproject.passmate.user.domain.model.MyGrade
import org.sesacteamproject.passmate.user.domain.model.MyPage
import org.sesacteamproject.passmate.user.domain.model.MyPageSummary
import org.sesacteamproject.passmate.user.domain.model.NotificationSettings
import org.sesacteamproject.passmate.user.domain.model.NextGrade
import org.sesacteamproject.passmate.user.domain.model.UserProfile

// 서버는 진행 중 방(ongoing)·추이 문구(trendText)를 주지 않는다 — 계약 갱신 대상.
fun MyPageResponse.toDomain(): MyPage {
    return MyPage(
        summary = summary.toDomain(),
        ongoing = null,
        rooms = rooms.content.map { it.toDomain() },
        nextCursor = if (rooms.hasNext) (rooms.page + 1).toString() else null
    )
}

fun MyPageResponse.SummaryDto.toDomain(): MyPageSummary {
    return MyPageSummary(
        participationCount = completedSessionCount,
        // 서버가 이미 0~100 퍼센트로 준다 (ParticipantReport.accuracyOf)
        accuracyPercent = averageAccuracy.roundToInt(),
        avgRank = averageRank,
        trendText = null,
        weakTopics = weakTopics
    )
}

fun MyPageResponse.RoomDto.toDomain(): JoinedRoom {
    return JoinedRoom(
        roomId = roomId,
        title = title,
        dateLabel = displayDate(endedAt ?: startedAt),
        questionCount = questionCount,
        myScore = myScore?.toDouble(),
        myRank = myRank,
        hasReport = hasReport
    )
}

// LocalDateTime 문자열("2026-07-18T21:10:00")의 날짜 부분을 화면 표기로 바꾼다.
// 시간대 변환이 필요 없는 표시용이라 문자열 처리로 충분하다.
private fun displayDate(isoDateTime: String?): String {
    val date = isoDateTime?.substringBefore("T")

    return if (date != null && date.length == 10) {
        date.replace("-", ".")
    } else {
        ""
    }
}

fun GradeResponse.toDomain(): MyGrade {
    return MyGrade(
        level = HostLevel.from(level) ?: HostLevel.SEEDLING,
        achievedAt = achievedAt,
        stats = stats.toDomain(),
        next = next?.toDomain()
    )
}

fun GradeResponse.StatsDto.toDomain(): GradeStats {
    return GradeStats(
        participationCount = participationCount,
        avgAccuracyPercent = avgAccuracyPercent,
        roomCount = roomCount,
        totalStudents = totalStudents,
        avgStars = avgStars,
        ratingCount = ratingCount
    )
}

fun GradeResponse.NextDto.toDomain(): NextGrade {
    return NextGrade(
        level = HostLevel.from(level) ?: HostLevel.MASTER,
        progressPercent = progressPercent.coerceIn(0, 100),
        criteria = criteria.map { it.toDomain() }
    )
}

fun GradeResponse.CriterionDto.toDomain(): GradeCriterion {
    return GradeCriterion(
        label = label,
        current = current,
        target = target,
        met = met
    )
}

// 계약에 없는 뱃지 타입은 버린다 — 서버가 새 뱃지를 추가해도 구버전 앱이 깨지지 않게
fun BadgesResponse.toDomain(): List<Badge> {
    return items.mapNotNull { item ->
        BadgeType.from(item.type)?.let { type ->
            Badge(
                type = type,
                earned = item.earned,
                earnedAt = item.earnedAt,
                progressCurrent = item.progressCurrent,
                progressTarget = item.progressTarget
            )
        }
    }
}

fun HostProfileResponse.toDomain(): HostProfile {
    return HostProfile(
        userId = userId,
        nickname = nickname,
        intro = intro,
        level = HostLevel.from(level),
        avgStars = avgStars,
        ratingCount = ratingCount,
        roomCount = roomCount,
        totalStudents = totalStudents,
        badges = badges.mapNotNull { BadgeType.from(it) },
        rooms = rooms.map { it.toDomain() }
    )
}

fun UserProfileResponse.toDomain(): UserProfile {
    return UserProfile(
        nickname = nickname,
        email = email,
        joinedAt = joinedAt,
        avatarId = avatarId,
        level = HostLevel.from(level),
        coins = coins,
        joinedRoomCount = joinedRoomCount,
        hostedRoomCount = hostedRoomCount
    )
}

fun NotificationSettingsDto.toDomain(): NotificationSettings {
    return NotificationSettings(
        sessionStart = sessionStart,
        ratingRequest = ratingRequest,
        settlementDone = settlementDone
    )
}
