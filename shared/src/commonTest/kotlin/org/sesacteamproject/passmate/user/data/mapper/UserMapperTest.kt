package org.sesacteamproject.passmate.user.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.sesacteamproject.passmate.room.domain.model.HostLevel
import org.sesacteamproject.passmate.user.data.dto.BadgesResponse
import org.sesacteamproject.passmate.user.data.dto.GradeResponse
import org.sesacteamproject.passmate.user.data.dto.HostProfileResponse
import org.sesacteamproject.passmate.user.data.dto.MyPageResponse
import org.sesacteamproject.passmate.user.domain.model.BadgeType

class UserMapperTest {

    @Test
    fun mapsSummaryOngoingAndRooms() {
        val response = MyPageResponse(
            summary = MyPageResponse.SummaryDto(
                participationCount = 3,
                accuracyPercent = 71,
                avgRank = 3.3,
                trendText = "지난주보다 정답률이 8%p 올랐어요",
                weakTopics = listOf("JPA 영속성", "트랜잭션")
            ),
            ongoing = MyPageResponse.OngoingDto(
                roomId = 9,
                pin = "482913",
                title = "Spring 실전 모의고사 4주차",
                hostNickname = "김선생",
                progressLabel = "3 / 8 문항 진행 중"
            ),
            rooms = listOf(
                MyPageResponse.RoomDto(
                    roomId = 1,
                    title = "Spring 스터디",
                    dateLabel = "8/22 (금)",
                    questionCount = 8,
                    myScore = 990.0,
                    myRank = 3,
                    hasReport = true
                )
            ),
            nextCursor = "c2"
        )

        val myPage = response.toDomain()

        assertEquals(3, myPage.summary.participationCount)
        assertEquals(71, myPage.summary.accuracyPercent)
        assertEquals(listOf("JPA 영속성", "트랜잭션"), myPage.summary.weakTopics)
        assertEquals(9L, myPage.ongoing?.roomId)
        assertEquals("482913", myPage.ongoing?.pin)
        assertEquals(1, myPage.rooms.size)
        assertEquals(990.0, myPage.rooms.first().myScore)
        assertEquals(true, myPage.rooms.first().hasReport)
        assertEquals("c2", myPage.nextCursor)
    }

    @Test
    fun mapsWithoutOngoingAndCursor() {
        val response = MyPageResponse(
            summary = MyPageResponse.SummaryDto(participationCount = 1, accuracyPercent = 50),
            rooms = emptyList()
        )

        val myPage = response.toDomain()

        assertNull(myPage.ongoing)
        assertNull(myPage.nextCursor)
        assertEquals(0, myPage.rooms.size)
    }

    @Test
    fun mapsGradeWithNextCriteria() {
        val response = GradeResponse(
            level = 2,
            stats = GradeResponse.StatsDto(
                participationCount = 18,
                avgAccuracyPercent = 72,
                roomCount = 12,
                totalStudents = 96,
                avgStars = 4.7,
                ratingCount = 34
            ),
            next = GradeResponse.NextDto(
                level = 3,
                progressPercent = 60,
                criteria = listOf(
                    GradeResponse.CriterionDto("방 운영 20회 이상", 12.0, 20.0, false),
                    GradeResponse.CriterionDto("평균 별점 4.0 이상", 4.7, 4.0, true)
                )
            )
        )

        val grade = response.toDomain()

        assertEquals(HostLevel.GROWING, grade.level)
        assertEquals(18, grade.stats.participationCount)
        assertEquals(HostLevel.VERIFIED, grade.next?.level)
        assertEquals(60, grade.next?.progressPercent)
        assertEquals(2, grade.next?.criteria?.size)
        assertEquals(true, grade.next?.criteria?.last()?.met)
    }

    @Test
    fun mapsTopGradeWithoutNext() {
        val grade = GradeResponse(level = 5).toDomain()

        assertEquals(HostLevel.MASTER, grade.level)
        assertNull(grade.next)
    }

    @Test
    fun dropsUnknownBadgeTypes() {
        val response = BadgesResponse(
            items = listOf(
                BadgesResponse.BadgeDto(type = "FIRST_ROOM", earned = true, earnedAt = "2026-08-01T00:00:00Z"),
                BadgesResponse.BadgeDto(type = "STREAK_30", earned = false, progressCurrent = 12, progressTarget = 30),
                BadgesResponse.BadgeDto(type = "FUTURE_BADGE", earned = true)
            )
        )

        val badges = response.toDomain()

        assertEquals(2, badges.size)
        assertEquals(BadgeType.FIRST_ROOM, badges.first().type)
        assertEquals(12, badges.last().progressCurrent)
    }

    @Test
    fun mapsHostProfileWithRooms() {
        val response = HostProfileResponse(
            userId = 7,
            nickname = "김민지",
            intro = "Spring · JPA · CS 면접 대비 방 운영 · 2026",
            level = 3,
            avgStars = 4.6,
            ratingCount = 128,
            roomCount = 24,
            totalStudents = 312,
            badges = listOf("FIRST_ROOM", "ROOMS_10", "UNKNOWN"),
            rooms = listOf(
                org.sesacteamproject.passmate.payment.data.dto.PublicRoomDto(
                    id = 1,
                    title = "백엔드 면접 스프린트",
                    host = org.sesacteamproject.passmate.payment.data.dto.PublicRoomHostDto(
                        userId = 7,
                        nickname = "김민지"
                    ),
                    type = "PAID",
                    fee = 10000
                )
            )
        )

        val profile = response.toDomain()

        assertEquals(HostLevel.VERIFIED, profile.level)
        assertEquals(2, profile.badges.size)
        assertEquals(1, profile.rooms.size)
        assertEquals(7L, profile.rooms.first().hostId)
    }
}
