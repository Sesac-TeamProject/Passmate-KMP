package org.sesacteamproject.passmate.user.data.mapper

import org.sesacteamproject.passmate.payment.data.mapper.toDomain
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.user.data.dto.BadgesResponse
import org.sesacteamproject.passmate.user.data.dto.GradeResponse
import org.sesacteamproject.passmate.user.data.dto.HostProfileResponse
import org.sesacteamproject.passmate.user.data.dto.MyPageResponse
import org.sesacteamproject.passmate.user.domain.model.Badge
import org.sesacteamproject.passmate.user.domain.model.BadgeType
import org.sesacteamproject.passmate.user.domain.model.GradeCriterion
import org.sesacteamproject.passmate.user.domain.model.GradeStats
import org.sesacteamproject.passmate.user.domain.model.HostProfile
import org.sesacteamproject.passmate.user.domain.model.JoinedRoom
import org.sesacteamproject.passmate.user.domain.model.MyGrade
import org.sesacteamproject.passmate.user.domain.model.MyPage
import org.sesacteamproject.passmate.user.domain.model.MyPageSummary
import org.sesacteamproject.passmate.user.domain.model.NextGrade
import org.sesacteamproject.passmate.user.domain.model.OngoingRoom

fun MyPageResponse.toDomain(): MyPage {
    return MyPage(
        summary = summary.toDomain(),
        ongoing = ongoing?.toDomain(),
        rooms = rooms.map { it.toDomain() },
        nextCursor = nextCursor
    )
}

fun MyPageResponse.SummaryDto.toDomain(): MyPageSummary {
    return MyPageSummary(
        participationCount = participationCount,
        accuracyPercent = accuracyPercent,
        avgRank = avgRank,
        trendText = trendText,
        weakTopics = weakTopics
    )
}

fun MyPageResponse.OngoingDto.toDomain(): OngoingRoom {
    return OngoingRoom(
        roomId = roomId,
        pin = pin,
        title = title,
        hostNickname = hostNickname,
        progressLabel = progressLabel
    )
}

fun MyPageResponse.RoomDto.toDomain(): JoinedRoom {
    return JoinedRoom(
        roomId = roomId,
        title = title,
        dateLabel = dateLabel,
        questionCount = questionCount,
        myScore = myScore,
        myRank = myRank,
        hasReport = hasReport
    )
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
