package org.sesacteamproject.passmate.user.data.mapper

import org.sesacteamproject.passmate.payment.data.mapper.toDomain
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.user.data.dto.BadgesResponse
import org.sesacteamproject.passmate.user.data.dto.GradeResponse
import org.sesacteamproject.passmate.user.data.dto.HostProfileResponse
import org.sesacteamproject.passmate.user.data.dto.UserProfileResponse
import kotlin.math.roundToInt
import org.sesacteamproject.passmate.core.model.DisplayDate
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
        dateLabel = DisplayDate.format(endedAt ?: startedAt) ?: "",
        questionCount = questionCount,
        myScore = myScore?.toDouble(),
        myRank = myRank,
        hasReport = hasReport
    )
}

// LocalDateTime 문자열("2026-07-18T21:10:00")의 날짜 부분을 화면 표기로 바꾼다.}

// 서버는 평면 구조로 준다. 참여 횟수·정답률은 이 응답에 없어 0/null로 둔다 (계약 갱신 대상).
fun GradeResponse.toDomain(): MyGrade {
    return MyGrade(
        level = HostLevel.from(level) ?: HostLevel.SEEDLING,
        achievedAt = levelAchievedAt,
        stats = GradeStats(
            participationCount = 0,
            avgAccuracyPercent = null,
            roomCount = roomsHosted,
            totalStudents = totalStudents,
            avgStars = avgRating,
            ratingCount = ratingCount
        ),
        next = nextGradeOrNull()
    )
}

// nextLevel이 없으면 최고 등급이라 다음 승급 정보가 없다
private fun GradeResponse.nextGradeOrNull(): NextGrade? {
    val next = nextLevel

    return if (next == null) {
        null
    } else {
        NextGrade(
            level = HostLevel.from(next) ?: HostLevel.MASTER,
            // 서버는 0.0~1.0 비율로 준다 — 화면은 퍼센트로 그린다
            progressPercent = ((nextLevelProgress ?: 0.0) * 100).roundToInt().coerceIn(0, 100),
            criteria = nextRequirements.map { it.toDomain() }
        )
    }
}

fun GradeResponse.RequirementDto.toDomain(): GradeCriterion {
    return GradeCriterion(
        label = label,
        current = current,
        target = target,
        met = met
    )
}

// 서버가 모르는 코드를 주면 화면에서 접는다 (뱃지 8종은 계약 §BadgeType)
fun BadgesResponse.toDomain(): List<Badge> {
    return badges.mapNotNull { item ->
        BadgeType.from(item.code)?.let { type ->
            Badge(
                type = type,
                earned = item.achieved,
                earnedAt = item.achievedAt,
                progressCurrent = item.progress,
                progressTarget = item.target?.toInt()
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
