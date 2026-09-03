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
    fun mapsSummaryAndRooms() {
        val response = MyPageResponse(
            summary = MyPageResponse.SummaryDto(
                completedSessionCount = 3,
                averageAccuracy = 71.4,
                averageRank = 3.3,
                weakTopics = listOf("JPA 영속성", "트랜잭션")
            ),
            rooms = MyPageResponse.RoomPageDto(
                content = listOf(
                    MyPageResponse.RoomDto(
                        roomId = 1,
                        title = "Spring 스터디",
                        endedAt = "2026-08-22T21:10:00",
                        questionCount = 8,
                        myScore = 990,
                        myRank = 3,
                        hasReport = true
                    )
                ),
                page = 0,
                hasNext = true
            )
        )

        val myPage = response.toDomain()

        assertEquals(3, myPage.summary.participationCount)
        assertEquals(71, myPage.summary.accuracyPercent)
        assertEquals(listOf("JPA 영속성", "트랜잭션"), myPage.summary.weakTopics)
        assertEquals(1, myPage.rooms.size)
        assertEquals("2026.08.22", myPage.rooms.first().dateLabel)
        assertEquals(990.0, myPage.rooms.first().myScore)
        assertEquals(true, myPage.rooms.first().hasReport)
        assertEquals("1", myPage.nextCursor)
    }

    @Test
    fun mapsLastPageWithoutCursor() {
        val response = MyPageResponse(
            summary = MyPageResponse.SummaryDto(completedSessionCount = 1, averageAccuracy = 50.0),
            rooms = MyPageResponse.RoomPageDto(content = emptyList(), hasNext = false)
        )

        val myPage = response.toDomain()

        // 서버가 진행 중 방을 주지 않으므로 항상 null이다
        assertNull(myPage.ongoing)
        assertNull(myPage.nextCursor)
        assertEquals(0, myPage.rooms.size)
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
