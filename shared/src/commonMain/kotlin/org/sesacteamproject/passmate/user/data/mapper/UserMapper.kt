package org.sesacteamproject.passmate.user.data.mapper

import org.sesacteamproject.passmate.user.data.dto.MyPageResponse
import org.sesacteamproject.passmate.user.domain.model.JoinedRoom
import org.sesacteamproject.passmate.user.domain.model.MyPage
import org.sesacteamproject.passmate.user.domain.model.MyPageSummary
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
